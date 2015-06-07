package net.quux00;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.zip.Adler32;

/**
 * DOCUMENT ME
 * 
 * TODO:
 * + support non-powers-of-two trees
 * + write deserialize method and test it
 */
public class MerkleTree {

  public static final int MAGIC_HDR = 0xcdaace99;
  public static final int INT_BYTES = 4;
  public static final int LONG_BYTES = 8;
  public static final byte LEAF_SIG_TYPE = 0x0;
  public static final byte INTERNAL_SIG_TYPE = 0x01;
  
  final Adler32 crc = new Adler32();
  List<String> leafSigs;
  Node root;
  int depth;
  int nnodes;
  
  public MerkleTree() {}
  
  /**
   * Use this constructor when you have already constructed the tree (from deserialization).
   * @param treeRoot
   * @param numNodes
   * @param height
   * @param leafSignatures
   */
  public MerkleTree(Node treeRoot, int numNodes, int height, List<String> leafSignatures) {
    root = treeRoot;
    nnodes = numNodes;
    depth = height;
    leafSigs = leafSignatures;
  }
  
  
  /**
   * Serialization format:
   * (magicheader:int)(numnodes:int)[(nodetype:byte)(siglength:int)(signature:[]byte)]
   * @return
   */
  public byte[] serialize() {
    // header sizer
    int magicHeaderSz = INT_BYTES;
    int nnodesSz = INT_BYTES;
    int hdrSz = magicHeaderSz + nnodesSz;

    int typeByteSz = 1;
    int siglength = INT_BYTES;
    
    int parentSigSz = LONG_BYTES;
    int leafSigSz = leafSigs.get(0).getBytes(StandardCharsets.UTF_8).length;

    int maxSigSz = leafSigSz;
    if (parentSigSz > maxSigSz) {
      maxSigSz = parentSigSz;
    }
        
    int spaceForNodes = (typeByteSz + siglength + maxSigSz) * nnodes; 
    
    int cap = hdrSz + spaceForNodes;
    ByteBuffer buf = ByteBuffer.allocate(cap);
    
    buf.putInt(MAGIC_HDR).putInt(nnodes);  // header
    serializeBreadthFirst(buf);

    // the ByteBuf allocated space is likely more than was needed
    // so copy to a byte array of the exact size necesssary
    byte[] serializedTree = new byte[buf.position()];
    buf.rewind();
    buf.get(serializedTree);
    return serializedTree;
  }
  

  /**
   * Serialization format after the header section:
   * [(nodetype:byte)(siglength:int)(signature:[]byte)]
   * @param buf
   */
  void serializeBreadthFirst(ByteBuffer buf) {
    Queue<Node> q = new ArrayDeque<Node>((nnodes / 2) + 1);
    q.add(root);
    
    while (!q.isEmpty()) {
      Node nd = q.remove();
      buf.put(nd.type).putInt(nd.sig.length).put(nd.sig);
      
      if (nd.left != null) {
        q.add(nd.left);
      }
      if (nd.right != null) {
        q.add(nd.right);
      }
    }
  }

  /**
   * For now we assume leafSigs is a power of two
   */
  public void constructTree(List<String> signatures) {
    if (signatures.size() <= 1) {
      throw new IllegalArgumentException("Must be at least two signatures to construct a Merkle tree");
    }
    
    leafSigs = signatures;
    nnodes = signatures.size();
    List<Node> parents = bottomLevel(signatures);
    nnodes += parents.size();
    depth = 1;
    
    while (parents.size() > 1) {
      parents = nextLevel(parents);
      depth++;
      nnodes += parents.size();
    }
    
    root = parents.get(0);
  }

  
  public int getNumNodes() {
    return nnodes;
  }
  
  public Node getRoot() {
    return root;
  }
  
  public int getHeight() {
    return depth;
  }
  

  List<Node> nextLevel(List<Node> children) {
    List<Node> parents = new ArrayList<Node>(children.size() / 2);
    
    for (int i = 0; i < children.size() - 1; i += 2) {
      Node child1 = children.get(i);
      Node child2 = children.get(i+1);
      
      Node parent = constructInternalNode(child1, child2);
      parents.add(parent);
    }
    
    if (children.size() % 2 != 0) {
      Node child = children.get(children.size()-1);
      Node parent = constructInternalNode(child, null);
      parents.add(parent);
    }
    
    return parents;
  }

  
  /**
   * Constructs the bottom part of the tree - the leaf nodes and their
   * immediate parents.  Returns a list of the parent nodes.
   */
  List<Node> bottomLevel(List<String> signatures) {
    List<Node> parents = new ArrayList<Node>(signatures.size() / 2);
    
    for (int i = 0; i < signatures.size() - 1; i += 2) {
      Node leaf1 = constructLeafNode(signatures.get(i));
      Node leaf2 = constructLeafNode(signatures.get(i+1));
      
      Node parent = constructInternalNode(leaf1, leaf2);
      parents.add(parent);
    }
    
    // if odd number of leafs, handle last entry
    if (signatures.size() % 2 != 0) {
      Node leaf = constructLeafNode(signatures.get(signatures.size() - 1));      
      Node parent = constructInternalNode(leaf, null);
      parents.add(parent);
    }
    
    return parents;
  }

  private Node constructInternalNode(Node child1, Node child2) {
    Node parent = new Node();
    parent.type = INTERNAL_SIG_TYPE;
    
    if (child2 == null) {
      parent.sig = child1.sig;
    } else {
      parent.sig = internalHash(child1.sig, child2.sig);
    }
    
    parent.left = child1;
    parent.right = child2;
    return parent;
  }

  private static Node constructLeafNode(String signature) {
    Node leaf = new Node();
    leaf.type = LEAF_SIG_TYPE;
    leaf.sig = signature.getBytes(StandardCharsets.UTF_8);
    return leaf;
  }

  
  
  byte[] internalHash(byte[] leftChildSig, byte[] rightChildSig) {
    crc.reset();
    crc.update(leftChildSig);
    crc.update(rightChildSig);
    return longToByteArray(crc.getValue());
  }

  static class Node {
    public byte type;  // INTERNAL_SIG_TYPE or LEAF_SIG_TYPE
    public byte[] sig; // signature of the node
    public Node left;
    public Node right;
    
    @Override
    public String toString() {
      String leftType = "<null>";
      String rightType = "<null>";
      if (left != null) {
        leftType = String.valueOf(left.type);
      }
      if (right != null) {
        rightType = String.valueOf(right.type);
      }
      return String.format("MerkleTree.Node<type:%d, sig:%s, left (type): %s, right (type): %s>",
          type, sigAsString(), leftType, rightType);
    }

    private String sigAsString() {
      StringBuffer sb = new StringBuffer();
      sb.append('[');
      for (int i = 0; i < sig.length; i++) {
        sb.append(sig[i]).append(' ');
      }
      sb.insert(sb.length()-1, ']');
      return sb.toString();
    }
  }  
  
  /**
   * Big-endian conversion
   */
  public static byte[] longToByteArray(long value) {
    return new byte[] {
        (byte) (value >> 56),
        (byte) (value >> 48),
        (byte) (value >> 40),
        (byte) (value >> 32),
        (byte) (value >> 24),
        (byte) (value >> 16),
        (byte) (value >> 8),
        (byte) value
    };
  }
}
