package com.sambarnum.trie;

import junit.framework.TestCase;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sbarnum
 */
public class TrieTest extends TestCase {
	private static final Logger log = Logger.getLogger(TrieTest.class.getName());

	private Trie trie;
	private static final Iterable<String> SAMPLE_WORDS = Arrays.asList("zap", "bingo", "bing", "ding", "dingo", "binding", "bonding");
	private int intsWritten = 0;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		trie = new Trie(SAMPLE_WORDS);
	}

	public void testContainsWord() throws Exception {
		for (String eachWord : SAMPLE_WORDS) {
			assertTrue("Did not find " + eachWord, trie.containsWord(eachWord));
		}
		assertFalse(trie.containsWord("binge"));
		assertFalse(trie.containsWord("bindi"));
		assertFalse(trie.containsWord("bin"));
		assertFalse(trie.containsWord("zin"));
		assertFalse(trie.containsWord("fin"));
		assertFalse(trie.containsWord("gin"));
		assertFalse(trie.containsWord("onding"));
	}

	public void testContainsPrefix() throws Exception {
		assertTrue(trie.containsPrefix("z"));
		assertTrue(trie.containsPrefix("za"));
		assertFalse(trie.containsPrefix("zap"));
		assertFalse(trie.containsPrefix("zapped"));
		assertTrue(trie.containsPrefix("d"));
		assertFalse(trie.containsPrefix("da"));
	}

	public void testVisit() throws Exception {
		final HashSet<String> tmpWords = new HashSet<>((java.util.Collection<? extends String>) SAMPLE_WORDS);
		trie.traverse(new TrieVisitor() {
			@Override
			public void visit(final CharSequence word, final Trie.Node node) {
				assertTrue(tmpWords.remove(word.toString()));
			}
		});
		assertTrue(tmpWords.isEmpty());
	}

	public void testVisitWithPrefix() throws Exception {
		List<String> bi = trie.toList("bi");
		assertEquals(Arrays.asList("binding", "bing", "bingo"), bi);
		bi = trie.toList("bin");
		assertEquals(Arrays.asList("binding", "bing", "bingo"), bi);
		bi = trie.toList("bing");
		assertEquals(Arrays.asList("bing", "bingo"), bi);
		bi = trie.toList("binge");
		assertTrue(bi.isEmpty());
		bi = trie.toList("zzz32");
		assertTrue(bi.isEmpty());
	}

	public void testSimpleCompress() throws Exception {
		final List<String> words = Arrays.asList("everyone", "one", "three", "ton", "two");
		final Trie trie = new Trie(words);
		int beforeCount = distinctNodeCount(trie);
		// suffixes: [eerht, eno, enoyreve, not, owt]
		trie.compress();
		int afterCount = distinctNodeCount(trie);
		log.log(Level.INFO, "Shrank from " + beforeCount + " to " + afterCount);
		assertTrue(afterCount < beforeCount);
		assertEquals(words, trie.toList(null));
		assertSame(trie.getRoot().find("everyone", 0), trie.getRoot().find("one", 0));
		assertSame(trie.getRoot().find("everyone", 0), trie.getRoot().find("three", 0));
		assertSame(trie.getRoot().find("everyon", 0), trie.getRoot().find("on", 0));
		assertSame(trie.getRoot().find("everyo", 0), trie.getRoot().find("o", 0));
		assertNotSame(trie.getRoot().find("everyo", 0), trie.getRoot().find("two", 0));
		assertSame(trie.getRoot().find("one", 0), trie.getRoot().find("three", 0));
		assertNotSame(trie.getRoot().find("o", 0), trie.getRoot().find("two", 0));
	}

	private int distinctNodeCount(final Trie trie) {
		new IdentityHashMap<>();
		return _distinctNodeCountRecursive(trie.getRoot(), new IdentityHashMap<Trie.Node, Object>());
	}

	private int _distinctNodeCountRecursive(final Trie.Node node, final IdentityHashMap<Trie.Node, Object> map) {
		int result = 0;
		if (map.put(node, Boolean.TRUE) == null) {
			result++;
		}
		if (node.children != null) {
			for (Trie.Node child : node.children) {
				result += _distinctNodeCountRecursive(child, map);
			}
		}
		return result;
	}

	public void testCompress() throws Exception {
		final List<String> strings = trie.toList(null);
		final List<String> words = Arrays.asList(
				"qat",
				"cat",
				"cats",
				"bat",
				"bats",
				"bots",
				"bob",
				"hub"
		);
		trie = new Trie(words);

		String beforeCompress = trie.toList(null).toString();


		int beforeCount = distinctNodeCount(trie);
		trie.compress();
		int afterCount = distinctNodeCount(trie);
		log.log(Level.INFO, "Shrank from " + beforeCount + " to " + afterCount);
		assertTrue(afterCount < beforeCount);

		String afterCompress = trie.toList(null).toString();

		assertEquals(beforeCompress, afterCompress);

		for (String word : words) {
			assertTrue(trie.containsWord(word));
		}

		assertFalse(trie.containsWord("ca"));
		assertFalse(trie.containsWord("qats"));
		final Trie.Node ca = trie.getRoot().find("ca", 0);
		assertNotNull(ca);
		assertFalse(ca.isEndOfWord());

		// [bat, bats, bob, bots, cat, cats, hub, qat]
		// suffixes: [bob, buh, stab, stac, stob, tab, tac, taq]

		assertSame(trie.getRoot().find("bat", 0), trie.getRoot().find("cat", 0));
		assertNotSame(trie.getRoot().find("bat", 0), trie.getRoot().find("bot", 0)); // there is a bots but no bot
		assertNotSame(trie.getRoot().find("bat", 0), trie.getRoot().find("qat", 0)); // there is no qats
		assertSame(trie.getRoot().find("bats", 0), trie.getRoot().find("cats", 0));
		assertSame(trie.getRoot().find("bats", 0), trie.getRoot().find("bots", 0));
		assertSame(trie.getRoot().find("hub", 0), trie.getRoot().find("bob", 0));
	}

	public void testWriteJavascriptDataFile() throws Exception {
		trie = wordsWithFriendsTrie();
		log.log(Level.INFO, "Compressing " + distinctNodeCount(trie) + " nodes");
		trie.compress();
		log.log(Level.INFO, "New node count: " + distinctNodeCount(trie));
		writeTrieToPath();
	}

	public void testWriteSimpleJavascriptDataFile() throws Exception {
		trie = new Trie(Arrays.asList("cat", "zap"));
		trie.compress();
		writeTrieToPath();
	}

	private void writeTrieToPath() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream data = new DataOutputStream(baos);
		//writeInt(data, 0);
		//writeInt(data, 1);
		//writeInt(data, 11);
		//writeInt(data, 1<<31);
		final IdentityHashMap<Trie.Node, Integer> headerIndexByNode = new IdentityHashMap<>();
		_populateMap(trie.getRoot(), headerIndexByNode, 0);
		final ArrayList<Map.Entry<Trie.Node, Integer>> entries = (ArrayList<Map.Entry<Trie.Node, Integer>>) new ArrayList<>(headerIndexByNode.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<Trie.Node, Integer>>() {
			@Override
			public int compare(final Map.Entry<Trie.Node, Integer> o1, final Map.Entry<Trie.Node, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		for (Map.Entry<Trie.Node, Integer> entry : entries) {
			assertEquals(entry.getValue().intValue(), intsWritten);
			writeInt(data, entry.getKey().bitSet);
			for (Trie.Node child : entry.getKey().getChildren()) {
				writeInt(data, headerIndexByNode.get(child));
			}
		}
		//_dump(entries);
		data.close();
		final byte[] bytes = baos.toByteArray();
		final String encoded = new BASE64Encoder().encode(bytes);
		final String path = "/Users/sbarnum/htdocs/test/trie.data";
		final FileOutputStream fileOutputStream = new FileOutputStream(path);
		System.out.println("Wrote " + encoded.length() + " chars to " + path);
		fileOutputStream.write(baos.toByteArray());
		fileOutputStream.close();
	}

	/**
	 * Breadth-first traversal of nodes, assigning them a spot to write things out
	 * @param node
	 * @param headerIndexByNode
	 * @param nextHeaderIndex
	 * @return
	 */
	private int _populateMap(final Trie.Node node, final IdentityHashMap<Trie.Node, Integer> headerIndexByNode, int nextHeaderIndex) {
		if (!headerIndexByNode.containsKey(node)) {
			headerIndexByNode.put(node, nextHeaderIndex);
			nextHeaderIndex += 1 + node.getChildCount();
			for (Trie.Node child : node.getChildren()) {
				nextHeaderIndex = _populateMap(child, headerIndexByNode, nextHeaderIndex);
			}
		}
		return nextHeaderIndex;
	}

	private void _dump(final Trie.Node node, final DataOutputStream data, final IdentityHashMap<Trie.Node, Integer> headerIndexByNode) throws IOException {
		int header = node.bitSet;
		writeInt(data, header);
		if (node.children != null) {
			for (Trie.Node child : node.children) {
				writeInt(data, headerIndexByNode.get(child));
				_dump(child, data, headerIndexByNode);
			}
		}
	}

	private void writeInt(final DataOutputStream data, final int v) throws IOException {
		data.write((0xff & v));
		data.write(0xff & v >> 8);
		data.write(0xff & v >> 16);
		data.write(0xff & v >> 24);
		intsWritten++;
	}

	Trie wordsWithFriendsTrie() throws IOException {
		Trie wwf = new Trie();
		final String filename = "/com/sambarnum/trie/words-with-friends.txt";
		log.log(Level.INFO, "Building dictionary from " + filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(filename)));
		String eachLine;

		while ((eachLine = br.readLine()) != null) {
			if (Character.isLowerCase(eachLine.charAt(0))) {
				wwf.add(eachLine);
			}
		}
		return wwf;
	}
}
