function Trie(array) {
	/** index in array of the given word. charOffset and parentIndex are generally 0 until recursed through */
	function indexOf(word, charOffset, parentIndex) {
		if (charOffset == word.length) return parentIndex;
		var indexOff = indexOffset(array[parentIndex], bitFor(word.charCodeAt(charOffset)));
		if (indexOffset == -1) return -1;
		return indexOf(word, charOffset + 1, array[parentIndex + 1 + indexOff]);
	}

	function indexOffset(header, bit) {
		var result = 0;
		for (var i = 0; i < 26; i++) {
			if ((header & 1 << i) != 0) {
				var hasChild = (header & (1 << i)) != 0;
				if (bit == i) {
					return hasChild ? result : -1;
				} else if (hasChild) {
					result++;
				}
			}
		}
		return -1;
	}

	function bitFor(charcode) {
		return charcode - 97; // 97='a'
	}

	function charFor(bit) {
		return String.fromCharCode(bit+97);
	}

	return {
		/** Returns true if word exists in the dictionary, but not if word is only a prefix ot other valid words. */
		containsWord: function (word) {
			var index = indexOf(word, 0, 0);
			return (array[index] & (1 << 31)) != 0;
		},
		/** Returns an array of letters which form valid words or suffixes if appended to word. */
		children: function (word) {
			var result = [], header=0, i=0, index = indexOf(word, 0, 0);
			if (index != -1) {
				header = array[index];
				for (i=0; i<26; i++) {
					if ((header & (1 << i)) != 0) {
						result.push (charFor(i));
					}
				}
			}
			return result;
		}
	}

}
