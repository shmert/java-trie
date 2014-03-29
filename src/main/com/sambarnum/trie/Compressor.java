package com.sambarnum.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sbarnum
 */
class Compressor {
	private static final Logger log = Logger.getLogger(Compressor.class.getName());

	private final List<Trie.Node> sharedPrefixes = new ArrayList<>();

	public void compress(final Trie src) {
		combineSharedSuffixes(src);
		trimArraysRecursive(src.getRoot());
	}

	private void combineSharedSuffixes(final Trie src) {
		final Trie suffixes = new Trie();
		src.traverse(new TrieVisitor() {
			@Override
			public void visit(final CharSequence word, final Trie.Node node) {
				final CharSequence reverse = reverse(word);
				suffixes.add(reverse);
			}
		});
		traverseSuffixNodesRecursive(src, suffixes.getRoot(), 0);
	}

	private CharSequence reverse(final CharSequence word) {
		return new StringBuilder(word).reverse().toString();
	}

	private void traverseSuffixNodesRecursive(final Trie src, final Trie.Node suffixNode, final int depth) {
		if (suffixNode.children != null) {
			if (suffixNode.getChildCount() > 1) {
				// two word paths converged into a common suffix. This is a candidate for combination
				// FIX! ideally try to combine deepest suffixes first, and don't redundantly combine more shallow suffixes
				sharedPrefixes.clear();
				new Trie(suffixNode).traverse(new TrieVisitor() {
					@Override
					public void visit(final CharSequence suffix, final Trie.Node node) {
						if (depth >= suffix.length()) return;
						final CharSequence prefix = reverse(suffix.subSequence(depth, suffix.length())); // prefix common among more than one word in src
						//if (prefix.length() == 1) return;
						final Trie.Node eachPrefixNode = src.getRoot().find(prefix, 0);
						for (Trie.Node otherPrefix : sharedPrefixes) {
							if (otherPrefix.equals(eachPrefixNode)) {
								if (log.isLoggable(Level.FINE)) {
									log.log(Level.FINE, "Combining " + prefix + " with " + otherPrefix);
								}
								int bit = Trie.Node.bitFor(prefix.charAt(prefix.length()-1));
								final Trie.Node parent = src.getRoot().find(prefix.subSequence(0, prefix.length() - 1), 0);
								assert parent != null;
								parent.children.set(parent.indexOf(bit), otherPrefix);
								return;
							}
						}
						sharedPrefixes.add(eachPrefixNode);
					}
				});
			}

			for (Trie.Node child : suffixNode.children) {
				traverseSuffixNodesRecursive(src, child, depth+1);
			}
		}
	}

	private void trimArraysRecursive(final Trie.Node node) {
		if (node.children != null) {
			node.children.trimToSize();
			for (Trie.Node child : node.children) {
				trimArraysRecursive(child);
			}
		}
	}
}
