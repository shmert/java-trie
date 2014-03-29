package com.sambarnum.trie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author sbarnum
 */
public class Trie implements Serializable {

	private final Node root;
	private int size = -1;

	public Trie() {
		this(new Node());
		this.size = 0;
	}

	public Trie(final Node root) {
		this.root = root;
	}

	public Trie(final Iterable<? extends CharSequence> sampleWords) {
		this();
		for (CharSequence eachWord : sampleWords) {
			add(eachWord);
		}
	}

	public void add(@NotNull final CharSequence word) {
		root.add(word, 0);
		size++;
	}

	public boolean containsWord(@NotNull final CharSequence word) {
		Node found = root.find(word, 0);
		return found != null && found.isEndOfWord();
	}

	public boolean containsPrefix(@NotNull final CharSequence prefix) {
		Node found = root.find(prefix, 0);
		return found != null && found.hasChildren();
	}

	public int getSize() {
		return size; // FIX! lazily compute size if -1
	}

	public Node getRoot() {
		return root;
	}

	public void traverse(@NotNull final TrieVisitor visitor) {
		traverse(visitor, null);
	}

	public void traverse(@NotNull final TrieVisitor visitor, @Nullable final CharSequence prefix) {
		final Node node = prefix == null || prefix.length() == 0 ? root : root.find(prefix, 0);
		if (node != null) {
			_traverseRecursive(visitor, new StringBuilder(prefix == null ? "" : prefix), node);
		}
	}

	private void _traverseRecursive(final TrieVisitor visitor, final StringBuilder sb, @NotNull final Node node) {
		if (sb.length() != 0 && node.isEndOfWord()) {
			visitor.visit(sb, node);
		}
		for (int bit = 0; bit < 26; bit++) {
			final int index = node.indexOf(bit);
			if (index != -1) {
				Node child = node.children.get(index);
				sb.append(Node.charFor(bit));
				_traverseRecursive(visitor, sb, child);
				sb.setLength(sb.length() - 1);
			}
		}
	}

	public List<String> toList(@Nullable String prefix) {
		final ArrayList<String> strings = new ArrayList<>();
		traverse(new TrieVisitor() {
			@Override
			public void visit(final CharSequence word, final Node node) {
				strings.add(word.toString());
			}
		}, prefix);
		return strings;
	}

	public void compress() {
		new Compressor().compress(this);
	}

	static class Node {
		/**
		 * first bit is whether this node represents an end-of-word. The last bits represent which letters this node has as children.
		 */
		int bitSet;
		/**
		 * Sparse array of children for the node.
		 */
		ArrayList<Node> children;

		public void add(@NotNull final CharSequence word, final int offset) {
			if (offset == word.length()) {
				bitSet |= 1<<31;
			} else {
				Node child = findOrCreateChild(word.charAt(offset));
				child.add(word, offset+1);
			}
		}

		@NotNull
		private Node findOrCreateChild(final char c) {
			Node child = childWithChar(c);
			if (child == null) {
				if (children == null) {
					children = new ArrayList<Node>(2);
				}
				final int newBit = bitFor(c);
				bitSet |= (1 << newBit);
				children.add(indexOf(newBit), child = new Node());
			}
			return child;
		}

		@Nullable
		public Node childWithChar(final char c) {
			if (children == null) {
				return null;
			}
			final int bit = bitFor(c);
			final int index = indexOf(bit);
			return index == -1 ? null : children.get(index);
		}

		/**
		 * An int from 0-25 for the specified char
		 */
		static int bitFor(final char c) {
			return (int) c - (int) 'a';
		}

		private static char charFor(final int bit) {
			return (char) ((int) 'a' + bit);
		}

		/**
		 * The position in the children array of the child for the character identified by <code>bit</code>, or <code>-1</code> if this node hos no child for <code>bit</code>..
		 */
		int indexOf(final int bit) {
			int result = 0;
			for (int i=0; i<26; i++) {
				final boolean hasChild = (bitSet & (1 << i)) != 0;
				if (bit == i) {
					return hasChild ? result : -1;
				} else if (hasChild) {
					result++;
				}
			}
			throw new IllegalArgumentException("Invalid bit " + bit);
		}

		@Nullable
		Node find(@NotNull final CharSequence word, final int offset) {
			if (offset == word.length()) {
				return this;
			}
			Node child = childWithChar(word.charAt(offset));
			if (child == null) {
				return null;
			} else if (offset == word.length() - 1) {
				return child;
			} else {
				return child.find(word, offset + 1);
			}
		}

		public boolean hasChildren() {
			return children != null;
		}

		@Override
		public String toString() {
			String childrenString;
			if (hasChildren()) {
				StringBuilder sb = new StringBuilder();
				for (char c = 'a'; c <= 'z'; c++) {
					if (indexOf(bitFor(c)) != -1) {
						if (sb.length() != 0) sb.append(", ");
						sb.append(c);
					}
				}
				childrenString = sb.toString();
			} else {
				childrenString = "";
			}
			return "Node{" +
					"endOfWord=" + isEndOfWord() +
					", children=" + (childrenString) +
					'}';
		}

		public boolean isEndOfWord() {
			return (bitSet & 1<<31) != 0;
		}

		@NotNull
		public List<Node> getChildren() {
			return children == null ? Collections.<Node>emptyList() : children;
		}

		public int getChildCount() {
			return children == null ? 0 : children.size();
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Node node = (Node) o;

			if (bitSet != node.bitSet) return false;
			if (children != null ? !children.equals(node.children) : node.children != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = bitSet;
			result = 31 * result + (children != null ? children.hashCode() : 0);
			return result;
		}
	}
}
