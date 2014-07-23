java-trie
=========

Try the [demo](http://shmert.github.io/java-trie/demo.html), or read more about the [file format](http://shmert.github.io/java-trie/index.html).

There are two main components to this project.

Java Implementation
===================
A Java [Trie][trie] / [DAWG][dawg] implementation, focusing on memory efficiency for ASCII trees (letters a-z, lowercase).

This implementation uses a sparse array to store children, and an int bit set to indicate which children a given node has, and whether a node is a valid end-of-word.

It includes a <code>compress()</code> function which greatly reduces the Trie's memory footprint.

The Java implementation includes a way to dump the Trie structure to an efficient binary format. You can then load this binary file into a JavaScript array, and have fast and low-memory Trie lookup operations on the client.

Creating a Trie in Java
-----------------------

	trie = new MemoryEfficientTrie(Arrays.asList("cat", "zap"));
	trie.add("bat");
	trie.compress(); // merges shared suffixes, trims arrays
	trie.containsWord("cat"); // true
	trie.containsWord("cap"); // false

JavaScript Implementation
=========================

The JavaScript implementation loads a pre-generated binary file containing an optimal representation of the Trie data.

**Loading a Trie**

	var oReq = new XMLHttpRequest();
	oReq.open("GET", "trie.data", true);
	oReq.responseType = "arraybuffer";
	oReq.onload = function (oEvent) {
		var arrayBuffer = oReq.response; // Note: not oReq.responseText
		if (arrayBuffer) {
			var byteArray = new Int32Array(arrayBuffer);
			trie = new Trie(byteArray);
			trie.containsWord('yurt'); // true
			trie.containsWord('yur'); // false
			trie.containsPrefix('yur'); // true
			trie.containsPrefix('yuq'); // false
		}
	};
	oReq.send(null);

Binary File Format
==================
The binary format is an array of int objects.

Each node in the Trie is written to the array, first with the bit set value, followed by zero or more reference addresses for the node's children.
These reference addresses point to other indices in the data array.


[trie]:[http://en.wikipedia.org/wiki/Trie]
[dawg]:[http://en.wikipedia.org/wiki/Directed_acyclic_word_graph]