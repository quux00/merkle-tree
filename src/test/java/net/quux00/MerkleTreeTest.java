package net.quux00;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.Adler32;

import org.junit.Test;

public class MerkleTreeTest {

  @Test
  public void testConstructTree_4LeafEntries() {
    MerkleTree m4tree = construct4LeafTree();
    
    MerkleTree.Node root = m4tree.getRoot();
    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.put(root.sig);
    buf.rewind();
    // pop off the leading 0 or 1 byte
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    long rootSig = buf.getLong();
    assertTrue(rootSig > 1);
    
    assertEquals(2, m4tree.getHeight());
    assertEquals(7, m4tree.getNumNodes());
    
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);
    
    MerkleTree.Node lev2Node0 = lev1Node0.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node0.type);
    assertEquals("52e422506d8238ef3196b41db4c41ee0afd659b6", new String(lev2Node0.sig, UTF_8));
    
    MerkleTree.Node lev2Node1 = lev1Node0.right;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node1.type);
    assertEquals("6d0b51991ac3806192f3cb524a5a5d73ebdaacf8", new String(lev2Node1.sig, UTF_8));

    MerkleTree.Node lev2Node2 = lev1Node1.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node2.type);
    assertEquals("461848c8b70e5a57bd94008b2622796ec26db657", new String(lev2Node2.sig, UTF_8));

    MerkleTree.Node lev2Node3 = lev1Node1.right;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node3.type);
    assertEquals("c938037dc70d107b3386a86df7fef17a9983cf53", new String(lev2Node3.sig, UTF_8));

    // check that internal Node signatures are correct
    Adler32 adler = new Adler32();
    
    adler.update(lev2Node0.sig);
    adler.update(lev2Node1.sig);
    buf.clear();
    buf.put(lev1Node0.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev2Node2.sig);
    adler.update(lev2Node3.sig);
    buf.clear();
    buf.put(lev1Node1.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev1Node0.sig);
    adler.update(lev1Node1.sig);
    buf.clear();
    buf.put(root.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());    
  }  

  
  @Test
  public void test4and8LeafTreesHaveDifferentRootSigs() {
    MerkleTree m4tree = construct4LeafTree();
    MerkleTree m8tree = construct8LeafTree();

    assertTrue(m4tree.getRoot().sig != m8tree.getRoot().sig);
  }

  @Test
  public void testConstructTree_8LeafEntries() {
    MerkleTree m8tree = construct8LeafTree();
    
    MerkleTree.Node root = m8tree.getRoot();
    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.put(root.sig);
    buf.rewind();
    // pop off the leading 0 or 1 byte
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    long rootSig = buf.getLong();
    assertTrue(rootSig > 1);
    
    assertEquals(3, m8tree.getHeight());
    assertEquals(15, m8tree.getNumNodes());
    
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);

    MerkleTree.Node lev2Node0 = lev1Node0.left;
    MerkleTree.Node lev2Node1 = lev1Node0.right;
    MerkleTree.Node lev2Node2 = lev1Node1.left;
    MerkleTree.Node lev2Node3 = lev1Node1.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node0.type);    
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node1.type);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node2.type);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node3.type);

    MerkleTree.Node lev3Node0 = lev2Node0.left;
    MerkleTree.Node lev3Node1 = lev2Node0.right;
    MerkleTree.Node lev3Node2 = lev2Node1.left;
    MerkleTree.Node lev3Node3 = lev2Node1.right;
    MerkleTree.Node lev3Node4 = lev2Node2.left;
    MerkleTree.Node lev3Node5 = lev2Node2.right;
    MerkleTree.Node lev3Node6 = lev2Node3.left;
    MerkleTree.Node lev3Node7 = lev2Node3.right;

    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node0.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node1.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node2.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node3.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node4.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node5.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node6.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev3Node7.type);
    
    assertNull(lev3Node0.left);
    assertNull(lev3Node0.right);
    assertNull(lev3Node2.left);
    assertNull(lev3Node6.right);
    
    assertEquals("461848c8b70e5a57bd94008b2622796ec26db657", new String(lev3Node2.sig, UTF_8));
    assertEquals("994d89c38e5b9384235696a0efea5b6b93efb270", new String(lev3Node7.sig, UTF_8));
    
    // check some of the internal parent node signatures
    Adler32 adler = new Adler32();
    
    adler.update(lev3Node4.sig);
    adler.update(lev3Node5.sig);
    buf.clear();
    buf.put(lev2Node2.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev2Node2.sig);
    adler.update(lev2Node3.sig);
    buf.clear();
    buf.put(lev1Node1.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());
    
    adler.reset();
    adler.update(lev1Node0.sig);
    adler.update(lev1Node1.sig);
    buf.clear();
    buf.put(root.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());
  }


  @Test
  public void testConstructTree_9LeafEntries() {
    MerkleTree m9tree = construct9LeafTree();
    
    MerkleTree.Node root = m9tree.getRoot();
    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.put(root.sig);
    buf.rewind();
    // pop off the leading 0 or 1 byte
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    long rootSig = buf.getLong();
    assertTrue(rootSig > 1);
    
    assertEquals(4, m9tree.getHeight());
    assertEquals(20, m9tree.getNumNodes());
    
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);
    
    // right hand tree should just be a linked list of promoted node sigs
    assertNull(lev1Node1.right);
    MerkleTree.Node lev2Node2 = lev1Node1.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node2.type);
    assertArrayEquals(lev1Node1.sig, lev2Node2.sig);
    
    assertNull(lev2Node2.right);
    MerkleTree.Node lev3Node4 = lev2Node2.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev3Node4.type);
    assertArrayEquals(lev2Node2.sig, lev3Node4.sig);

    assertNull(lev3Node4.right);
    MerkleTree.Node lev4Node8 = lev3Node4.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node8.type);
    assertArrayEquals(lev3Node4.sig, lev4Node8.sig);

    // check some of the left hand trees to ensure correctness
    MerkleTree.Node lev2Node0 = lev1Node0.left;
    MerkleTree.Node lev3Node1 = lev2Node0.right;
    MerkleTree.Node lev4Node3 = lev3Node1.right;

    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node0.type);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev3Node1.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node3.type);
    assertNull(lev4Node3.left);
    assertNull(lev4Node3.right);

    // check some of the internal parent node signatures
    Adler32 adler = new Adler32();
    
    adler.update(lev3Node1.left.sig);
    adler.update(lev3Node1.right.sig);
    buf.clear();
    buf.put(lev3Node1.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev2Node0.left.sig);
    adler.update(lev2Node0.right.sig);
    buf.clear();
    buf.put(lev2Node0.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

  }

  
  @Test
  public void testConstructTree_10LeafEntries() {
    MerkleTree m9tree = construct10LeafTree();
    
    MerkleTree.Node root = m9tree.getRoot();
    ByteBuffer buf = ByteBuffer.allocate(8);
    buf.put(root.sig);
    buf.rewind();
    // pop off the leading 0 or 1 byte
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    long rootSig = buf.getLong();
    assertTrue(rootSig > 1);
    
    assertEquals(4, m9tree.getHeight());
    assertEquals(21, m9tree.getNumNodes());
    
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);
    
    
    // right hand tree should just be a linked list of promoted node sigs
    assertNull(lev1Node1.right);
    MerkleTree.Node lev2Node2 = lev1Node1.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node2.type);
    assertArrayEquals(lev1Node1.sig, lev2Node2.sig);
    
    assertNull(lev2Node2.right);
    MerkleTree.Node lev3Node4 = lev2Node2.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev3Node4.type);
    assertArrayEquals(lev2Node2.sig, lev3Node4.sig);

    // the bottom level has two nodes - only the first parent node was promoted
    MerkleTree.Node lev4Node8 = lev3Node4.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node8.type);
    assertFalse(new String(lev3Node4.sig, UTF_8).equals(new String(lev4Node8.sig, UTF_8)));
    
    MerkleTree.Node lev4Node9 = lev3Node4.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node9.type);
    assertFalse(new String(lev3Node4.sig, UTF_8).equals(new String(lev4Node9.sig, UTF_8)));

    Adler32 adler = new Adler32();
    
    adler.update(lev3Node4.left.sig);
    adler.update(lev3Node4.right.sig);
    buf.clear();
    buf.put(lev3Node4.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());
  }

  
  @Test
  public void testMerkleCanDetectOutOfOrderMessages() {
    MerkleTree order1m = construct4LeafTree();
    MerkleTree order2m = construct4LeafTreeOrder2();
    MerkleTree order3m = construct4LeafTreeOrder3();
    MerkleTree order3mb = construct4LeafTreeOrder3();
    
    assertArrayEquals(order3m.getRoot().sig, order3mb.getRoot().sig);
    
    long order1RootSig = convertSigToLong(order1m.getRoot().sig);
    long order2RootSig = convertSigToLong(order2m.getRoot().sig);
    long order3RootSig = convertSigToLong(order3m.getRoot().sig);
    assertNotEquals(order1RootSig, order2RootSig);
    assertNotEquals(order1RootSig, order3RootSig);
    assertNotEquals(order2RootSig, order3RootSig);
  }

  
  /* ---[ ser / deserialization tests ]--- */
  
  @Test
  public void testSerializationDeserialization4LeafTree() {
    MerkleTree m4tree = construct4LeafTree();
    byte[] serializedTree = m4tree.serialize();
    
    ByteBuffer buf = ByteBuffer.wrap(serializedTree);
    assertEquals(MerkleTree.MAGIC_HDR, buf.getInt());
    assertEquals(m4tree.getNumNodes(), buf.getInt());
    
    // root node
    MerkleTree.Node root = new MerkleTree.Node();
    root.type = buf.get();
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    int len = buf.getInt();
    assertEquals(MerkleTree.LONG_BYTES, len);
    root.sig = new byte[len];
    buf.get(root.sig);
    long rootSig = convertSigToLong(root.sig);
    long expectedRootSig = convertSigToLong(m4tree.getRoot().sig);
    assertEquals(expectedRootSig, rootSig);

  
    MerkleTree dtree = MerkleDeserializer.deserialize(serializedTree);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, dtree.getRoot().type);
    assertArrayEquals(root.sig, dtree.getRoot().sig);
    
    root = dtree.getRoot();
    
    assertEquals(2, dtree.getHeight());
    assertEquals(7, dtree.getNumNodes());
    
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);
    
    MerkleTree.Node lev2Node0 = lev1Node0.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node0.type);
    assertEquals("52e422506d8238ef3196b41db4c41ee0afd659b6", new String(lev2Node0.sig, UTF_8));
    
    MerkleTree.Node lev2Node1 = lev1Node0.right;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node1.type);
    assertEquals("6d0b51991ac3806192f3cb524a5a5d73ebdaacf8", new String(lev2Node1.sig, UTF_8));

    MerkleTree.Node lev2Node2 = lev1Node1.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node2.type);
    assertEquals("461848c8b70e5a57bd94008b2622796ec26db657", new String(lev2Node2.sig, UTF_8));

    MerkleTree.Node lev2Node3 = lev1Node1.right;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev2Node3.type);
    assertEquals("c938037dc70d107b3386a86df7fef17a9983cf53", new String(lev2Node3.sig, UTF_8));

    // check that internal Node signatures are correct
    Adler32 adler = new Adler32();
    
    adler.update(lev2Node0.sig);
    adler.update(lev2Node1.sig);
    buf.clear();
    buf.put(lev1Node0.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev2Node2.sig);
    adler.update(lev2Node3.sig);
    buf.clear();
    buf.put(lev1Node1.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev1Node0.sig);
    adler.update(lev1Node1.sig);
    buf.clear();
    buf.put(root.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());    
  }
  
  @Test
  public void testSerializationDeserialization9LeafTree() {
    MerkleTree m9tree = construct9LeafTree();
    byte[] serializedTree = m9tree.serialize();
    
    ByteBuffer buf = ByteBuffer.wrap(serializedTree);
    assertEquals(MerkleTree.MAGIC_HDR, buf.getInt());
    assertEquals(m9tree.getNumNodes(), buf.getInt());
    
    // root node
    MerkleTree.Node root = new MerkleTree.Node();
    root.type = buf.get();
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    int len = buf.getInt();
    assertEquals(MerkleTree.LONG_BYTES, len);
    root.sig = new byte[len];
    buf.get(root.sig);
    long rootSig = convertSigToLong(root.sig);
    long expectedRootSig = convertSigToLong(m9tree.getRoot().sig);
    assertEquals(expectedRootSig, rootSig);

  
    MerkleTree dtree = MerkleDeserializer.deserialize(serializedTree);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, dtree.getRoot().type);
    assertArrayEquals(root.sig, dtree.getRoot().sig);
    
    assertEquals(4, dtree.getHeight());
    assertEquals(20, dtree.getNumNodes());

    root = dtree.getRoot();
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node0.type);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev1Node1.type);
    
    // right hand tree should just be a linked list of promoted node sigs
    assertNull(lev1Node1.right);
    MerkleTree.Node lev2Node2 = lev1Node1.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node2.type);
    assertArrayEquals(lev1Node1.sig, lev2Node2.sig);
    
    assertNull(lev2Node2.right);
    MerkleTree.Node lev3Node4 = lev2Node2.left;
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev3Node4.type);
    assertArrayEquals(lev2Node2.sig, lev3Node4.sig);

    assertNull(lev3Node4.right);
    MerkleTree.Node lev4Node8 = lev3Node4.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node8.type);
    assertArrayEquals(lev3Node4.sig, lev4Node8.sig);

    // check some of the left hand trees to ensure correctness
    MerkleTree.Node lev2Node0 = lev1Node0.left;
    MerkleTree.Node lev3Node1 = lev2Node0.right;
    MerkleTree.Node lev4Node3 = lev3Node1.right;

    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev2Node0.type);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, lev3Node1.type);
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev4Node3.type);
    assertNull(lev4Node3.left);
    assertNull(lev4Node3.right);

    // check some of the internal parent node signatures
    Adler32 adler = new Adler32();
    
    adler.update(lev3Node1.left.sig);
    adler.update(lev3Node1.right.sig);
    buf.clear();
    buf.put(lev3Node1.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());

    adler.reset();
    adler.update(lev2Node0.left.sig);
    adler.update(lev2Node0.right.sig);
    buf.clear();
    buf.put(lev2Node0.sig);
    buf.rewind();
    assertEquals(buf.getLong(), adler.getValue());
  }
  
  
  @Test
  public void testSerializationDeserialization2LeafTree() {
    MerkleTree m2tree = construct2LeafTree();
    byte[] serializedTree = m2tree.serialize();
    
    ByteBuffer buf = ByteBuffer.wrap(serializedTree);
    assertEquals(MerkleTree.MAGIC_HDR, buf.getInt());
    assertEquals(m2tree.getNumNodes(), buf.getInt());
    
    // root node
    MerkleTree.Node root = new MerkleTree.Node();
    root.type = buf.get();
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    int len = buf.getInt();
    assertEquals(MerkleTree.LONG_BYTES, len);
    root.sig = new byte[len];
    buf.get(root.sig);
    long rootSig = convertSigToLong(root.sig);
    long expectedRootSig = convertSigToLong(m2tree.getRoot().sig);
    assertEquals(expectedRootSig, rootSig);


    // deserialize
    MerkleTree dtree = MerkleDeserializer.deserialize(serializedTree);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, dtree.getRoot().type);
    assertArrayEquals(root.sig, dtree.getRoot().sig);
    
    assertEquals(1, dtree.getHeight());
    assertEquals(3, dtree.getNumNodes());

    root = dtree.getRoot();
    MerkleTree.Node lev1Node0 = root.left;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev1Node0.type);
    assertNull(lev1Node0.left);
    assertNull(lev1Node0.right);
    
    MerkleTree.Node lev1Node1 = root.right;
    assertEquals(MerkleTree.LEAF_SIG_TYPE, lev1Node1.type);
    assertEquals("26fe8e189fd5bb3fe56d4d3def6494802cb8cba3", new String(lev1Node1.sig, UTF_8));
    assertNull(lev1Node1.left);
    assertNull(lev1Node1.right);
  }
  
  
  @Test
  public void testSerializationDeserialization1019LeafTree() {
    MerkleTree m1019tree = construct1019LeafTree();
    byte[] serializedTree = m1019tree.serialize();

    ByteBuffer buf = ByteBuffer.wrap(serializedTree);
    assertEquals(MerkleTree.MAGIC_HDR, buf.getInt());
    assertEquals(m1019tree.getNumNodes(), buf.getInt());
    assertTrue(m1019tree.getNumNodes() > 1019 * 2);
    assertEquals(10, m1019tree.getHeight());
    
    // root node
    MerkleTree.Node root = new MerkleTree.Node();
    root.type = buf.get();
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, root.type);
    int len = buf.getInt();
    assertEquals(MerkleTree.LONG_BYTES, len);
    root.sig = new byte[len];
    buf.get(root.sig);
    long rootSig = convertSigToLong(root.sig);
    long expectedRootSig = convertSigToLong(m1019tree.getRoot().sig);
    assertEquals(expectedRootSig, rootSig);
    
    
    // deserialize
    MerkleTree dtree = MerkleDeserializer.deserialize(serializedTree);
    assertEquals(MerkleTree.INTERNAL_SIG_TYPE, dtree.getRoot().type);
    assertArrayEquals(root.sig, dtree.getRoot().sig);
    
    assertEquals(10, dtree.getHeight());
    assertEquals(m1019tree.getNumNodes(), dtree.getNumNodes());
  }
  

  // must have at least two entries to construct a MerkleTree
  @SuppressWarnings("unused")
  @Test(expected=IllegalArgumentException.class)
  public void testConstruct1LeafTree() {
    List<String> signatures = Arrays.asList("abc");
    new MerkleTree(signatures);
  }
  
  /* ---[ helper methods ]--- */
  
  private static long convertSigToLong(byte[] sig) {
    ByteBuffer buf = ByteBuffer.wrap(sig);
    return buf.getLong();
  }

  MerkleTree construct2LeafTree() {
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "26fe8e189fd5bb3fe56d4d3def6494802cb8cba3"
        );
    return new MerkleTree(signatures);
  }

  MerkleTree construct1019LeafTree() {
    List<String> signatures = new ArrayList<>(1019);
    for (int i = 0; i < 1019; i++) {
      signatures.add(UUID.randomUUID().toString());
    }
    return new MerkleTree(signatures);
  }
  
  MerkleTree construct9LeafTree() {
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "461848c8b70e5a57bd94008b2622796ec26db657",
        "c938037dc70d107b3386a86df7fef17a9983cf53",
        "d9312928e5702168348fe67ee2a3e3a1b7bc7c93",
        "506d93ebff5365d8f5dd9fedd4a063949be831a4",
        "e45922755802b52f11599d4746035ecad18c0c46",
        "994d89c38e5b9384235696a0efea5b6b93efb270",
        "26fe8e189fd5bb3fe56d4d3def6494802cb8cba3"
        );
    return new MerkleTree(signatures);
  }


  MerkleTree construct10LeafTree() {
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "461848c8b70e5a57bd94008b2622796ec26db657",
        "c938037dc70d107b3386a86df7fef17a9983cf53",
        "d9312928e5702168348fe67ee2a3e3a1b7bc7c93",
        "506d93ebff5365d8f5dd9fedd4a063949be831a4",
        "e45922755802b52f11599d4746035ecad18c0c46",
        "994d89c38e5b9384235696a0efea5b6b93efb270",
        "26fe8e189fd5bb3fe56d4d3def6494802cb8cba3",
        "3cf4172b27b7b182db0dd68276f08f7c27561c32"
        );
    return new MerkleTree(signatures);
  }

  
  MerkleTree construct8LeafTree() {
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "461848c8b70e5a57bd94008b2622796ec26db657",
        "c938037dc70d107b3386a86df7fef17a9983cf53",
        "d9312928e5702168348fe67ee2a3e3a1b7bc7c93",
        "506d93ebff5365d8f5dd9fedd4a063949be831a4",
        "e45922755802b52f11599d4746035ecad18c0c46",
        "994d89c38e5b9384235696a0efea5b6b93efb270"
        );
    return new MerkleTree(signatures);
  }
  
  MerkleTree construct4LeafTree() {
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "461848c8b70e5a57bd94008b2622796ec26db657",
        "c938037dc70d107b3386a86df7fef17a9983cf53");
    return new MerkleTree(signatures);
  }
  
  MerkleTree construct4LeafTreeOrder2() {
    // inverted [1] and [2] from the other order
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "461848c8b70e5a57bd94008b2622796ec26db657",
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "c938037dc70d107b3386a86df7fef17a9983cf53");
    return new MerkleTree(signatures);
  }

  MerkleTree construct4LeafTreeOrder3() {
    // inverted [2] and [3] from the other order
    List<String> signatures = Arrays.asList(
        "52e422506d8238ef3196b41db4c41ee0afd659b6", 
        "6d0b51991ac3806192f3cb524a5a5d73ebdaacf8",
        "c938037dc70d107b3386a86df7fef17a9983cf53",
        "461848c8b70e5a57bd94008b2622796ec26db657");
    return new MerkleTree(signatures);
  }

}
