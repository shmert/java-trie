package com.sambarnum.trie;

/**
 * @author sbarnum
 */
public interface TrieVisitor {
	void visit(CharSequence word, Trie.Node node);
}
