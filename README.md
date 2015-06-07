# Overview

I recently hit upon the need to do checkpointing in a data processing system that has the requirement that no data event can ever be lost and no events can be processed and streamed out of order.  I wanted a way to auto-detect this in production in real time.

There are a couple of ways to do this, but since our data events already have a signature attached to them (a SHA1 hash), I decided that a useful way to do the checkpoint is basically keep a hash of hashes.  One could do this with a [hash list](https://en.wikipedia.org/wiki/Hash_list), where a chain of hashes for each data element is kept and when a checkpoint occurs the hash of all those hashes **in order** is taken.

A disadvantage of this model is if the downstream system detects a hash mismatch (either due to a lost message or messages that are out-of-order) it would then have to iterate the full list to detect where the problem is.

An elegant alternative is a hash tree, aka a **Merkle Tree** named after its inventor Ralph Merkle.

## Merkle Trees

Merkle trees are typically implemented as binary trees where each non-leaf node is a hash of the two nodes below it.  The leaves can either be the data itself or a hash/signature of the data.

Thus, if any difference at the root hash is detected between systems, a binary search can be done through the tree to determine which particular subtree has the problem.  Thus typically only `log(N)` nodes need to be inspected rather than all `N` nodes to find the problem area.

Merkle trees are particularly effective in distributed systems where two separate systems can compare the data on each node via a Merkle tree and quickly determine which data sets (subtrees) are lacking on one or the other system.  Then only the subset of missing data needs to be sent.  Cassandra, based on Amazon's Dynamo, for example, uses Merkle trees as an [anti-entropy measure](https://wiki.apache.org/cassandra/AntiEntropy) to detect inconsistencies between replicas.

The [Tree Hash EXchange format](http://adc.sourceforge.net/draft-jchapweske-thex-02.html) (THEX) is used in some peer-to-peer systems for file integrity verification.  In that system the internal (non-leaf) nodes are allowed to have a different hashing algorithm than the leaf nodes.  In the diagram below `IH=InternalHashFn` and `LH=LeafHashFn`.

<img src="https://docs.google.com/drawings/d/1HQDgOwt3LYc6YlgUk_fZ_gLoYbgfMmy_Sq6EwR4tobg/pub?w=532&amp;h=263">

The THEX system also defines a serialization format and format for dealing with incomplete trees.  The THEX system ensures that all leaves are at the same depth from the root node.  To do that it "promotes" nodes.  That is when a parent only has one child, it cannot does not take a hash of the child hash; instead it just "inherits" it.  If that is confusing, think of the Merkle tree as being built from the bottom up: all the leaves are present and hashes of hashes are built until a single root is present.

<img src="https://docs.google.com/drawings/d/1b-E2iWmhK3p5PaINOeNuvwNq6DLXXdk9nCujEdJm8Vw/pub?w=666&amp;h=366">

<small><b>Notation: The first token is a node label, followed by a conceptual value for the hash/signature of the node.  Note that E, H and J nodes all have the same signature, since they only have one child node.</b></small>


## Merkle Tree as Checkpoint Data
`
Before I describe the implementation, it will help to see the use case I'm targeting.

<img src="https://docs.google.com/drawings/d/1fTZBLqXlwg9eJQmfCNtiAWJ_hrtglmWGkwMFWaWW-yY/pub?w=651&amp;h=137">

The scenario above is a data processing pipeline where messages flow in one direction.  All the messages that come out of A go into B and are processed and transformed to some new value-added structure and sent on to C.  In between are queues to decouple the systems.

Throughput needs to be as high as possible and every message that comes out of A must be processed by B and sent to C in the same order.  No data events can be lost or reordered.  System A puts a signature (a SHA1 hash) into the metadata of the event and that metadata is present on the message event that C receives.

To ensure that all messages are received and in the correct order, a checkpoint is periodically created by A, summarizing all the messages sent since the last checkpoint.  That checkpoint message is put onto the Queue between A and B; B passes it downstream without alteration so that C can read it.  Between checkpoints, system C keeps a running list of all the events it has received so that it can compute the signatures necessary to validate what it has received against the checkpoint message that periodically comes in from A.



## My Implementation of a Merkle Tree


The THEX Merkle Tree design was the inspiration for my implementation, but for my use case I made some simplifying assumptions.  For one, I start with the leaves already having a signature.  Since THEX is designed for file integrity comparisons, it assumes that you have segmented a file into fixed size chunks.  That is not the use case I'm targeting.

The THEX algorithm "salts" the hash functions in order to ensure that there will be no collisions between the leaf hashes and the internal node hashes.  It concatenates the byte `0x01` to the internal hash and the byte `0x00` to the leaf hash:

    internal hash function = IH(X) = H(0x01, X)
    leaf hash function = LH(X) = H(0x00, X)

It is useful to be able to distinguish leaf from internal nodes (especially when deserializing), so I morphed this idea into one where each Node has a type byte -- `0x01` identifies an internal node and `0x00` identifies a leaf node.  This way I can leave the incoming leaf hashes intact for easier comparison by the downstream consumer.

So my `MerkleTree.Node` class is:

```java
static class Node {
  public byte type;  // INTERNAL_SIG_TYPE or LEAF_SIG_TYPE
  public byte[] sig; // signature of the node
  public Node left;
  public Node right;
}
```


## Hash/Digest Algorithm

Since the leaf nodes are being passed in, my MerkleTree does not know (or need to know) what hashing algorithm was used on the leaves.  Instead it only concerns itself with the internal leaf node digest algorithm.

The choice of hashing or digest algorithm is important, depending if you want to maximize performance or security.  If one is using a Merkle tree to ensure integrity of data between peers that should not trust one another, then security is paramount and a cryptographically secure hash, such as SHA-256, [Tiger](https://en.wikipedia.org/wiki/Tiger_%28cryptography%29), or SHA-3 should be used.

For my use case, I was not concerned with detecting malicious tampering.  I only need to detect data loss or reordering, and have as little impact on overall throughput as possible.  For that I can use a CRC rather than a full hashing algorithm.

Earlier I ran some benchmarks comparing the speed of Java implementations of SHA-1, Guava's Murmur hash, CRC32 and [Adler32](https://en.wikipedia.org/wiki/Adler-32).  Adler32 ([java.util.zip.Adler32](https://docs.oracle.com/javase/7/docs/api/java/util/zip/Adler32.html)) was the fastest of the bunch.  The typical use case for the Adler CRC is to detect data transmission errors.  It trades off reliability for speed, so it is the weakest choice, but I deemed it sufficient to detect the sort of error I was concerned with.

So in my implementation the Adler32 checksum is hard-coded into the codebase.  But if you want to change that we can either make the internal digest algorithm injectable or configurable or you can just copy the code and change it to use the algorithm you want.

The rest of the code is written to be agnostic of the hashing algorithm - all it deals with are the bytes of the signature.


## Serialization / Deserialization

My implementation has efficient binary serialization built into the MerkleTree and an accompanying `MerkleDeserializer` class that handles the deserialization.

I chose not to use the Java Serialization framework.  Instead the `serialize` method just returns an array of bytes and deserialize accepts that byte array.

The serialization format is:

    (magicheader:int)(numnodes:int)
    [(nodetype:byte)(siglength:int)(signature:[]byte)]


where `(foo:type)` indicates the name (foo) and the type/size of the serialized element.  I use a [magic header](https://en.wikipedia.org/wiki/Magic_number_%28programming%29) of `0xcdaace99` to allow the deserializer to be certain it has received a valid byte array.

The next number indicates the number of nodes in the tree.  Then follows an "array" of `numnodes` size where the elements are the node type (`0x01` for internal, `0x00` for leaf), the length of the signature and then the signature as an array of bytes `siglength` long.

By including the `siglength` field, I can allow leaf nodes signatures to be "promoted" to the parent internal node when there is an odd number of leaf nodes.  This allows the internal nodes to use signatures of different lengths.



## Usage

For the use case described above, you can imagine that system A does the following:

```java
List<String> eventSigs = new ArrayList<>();

while (true) {
  Event event = receiveEvent();
  String hash = computeHash(event);
  // ... process and transmit the message to the downstream Queue
  sendToDownstreamQueue(hash, event);

  eventSigs.add(has);

  if (isTimeForCheckpoint()) {
    MerkleTree mtree = new MerkleTree(eventSigs);
    eventSigs.clear();
    byte[] serializedTree = mtree.serialize();
    sendToDownstreamQueue(serializedTree);
  }
}
```  

And system C would then do something like:


```java
List<String> eventSigs = new ArrayList<>();

while (true) {
  Event event = receiveEvent();

  if (isCheckpointMessage(event)) {
    MerkleTree mytree = new MerkleTree(eventSigs);
    eventSigs.clear();

    byte[] treeBytes = event.getDataAsBytes();
    MerkleTree expectedTree = MerkleDeserializer.deserialize(treeBytes);
    byte[] myRootSig = mytree.getRoot().sig;
    byte[] expectedRootSig = expectedTree.getRoot().sig;
    if (!signaturesAreEqual(myRootSig, expectedRootSig)) {
      evaluateTreeDifferences(mytree, expectedTree);
      // ... send alert
    }

  } else {
    String hash = event.getOriginalSignature();
    eventSigs.add(hash);
    // .. do something with event
  }
}
```  



## LICENSE

The MIT License.
