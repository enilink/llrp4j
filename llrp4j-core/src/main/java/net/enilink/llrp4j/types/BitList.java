/*
 * Copyright 2007 ETH Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package net.enilink.llrp4j.types;

import java.util.BitSet;
import java.util.Objects;

import net.enilink.llrp4j.bitbuffer.BitBuffer;

/**
 * A list of bits used for binary representations in LLRP messages.
 *
 */
public class BitList {
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	private BitSet bits;
	private int length;

	/**
	 * Creates a new LLRPBitList object.
	 */
	public BitList() {
		bits = new BitSet(0);
		bits.clear(0);
		length = 0;
	}

	/**
	 * Bytes interpreted in order they appear in array.
	 *
	 * @param bytes
	 *            interpreted in order they appear in array
	 */
	public BitList(byte[] bytes) {
		bits = new BitSet(bytes.length * 8);
		length = bytes.length * 8;

		// iterate over all bytes
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			int bitPosition = 8 * i;
			// iterate over each bit of one byte
			for (int j = 0; j < 8; j++) {
				bits.set(bitPosition + j, (b & (1 << j)) != 0);
			}
		}
	}

	/**
	 * create BitList from String. Everything but '0' is interpreted as 1.
	 *
	 * @param bitString
	 *            to be decoded
	 */
	public BitList(String bitString) {
		bits = new BitSet(bitString.length());
		length = bitString.length();

		for (int i = 0; i < bitString.length(); i++) {
			bits.set(i, bitString.charAt(i) != '0');
		}
	}

	/**
	 * creates lit list with all bits set to 0.
	 *
	 * @param n
	 *            length of bit list
	 */
	public BitList(int n) {
		bits = new BitSet(n);
		length = n;
	}

	/**
	 * Add a bit to bit list. Length is increased by 1.
	 *
	 * @param bit
	 *            to be added
	 */
	public void add(boolean bit) {
		bits.set(length, bit);
		length++;
	}

	/**
	 * appends other bit list to this. This list gets changed.
	 *
	 * @param other
	 *            bit list
	 */
	public void append(BitList other) {
		int position = length;
		for (int i = 0; i < other.length; i++) {
			length++;
			bits.set(position + i, other.get(i));
		}
	}

	/**
	 * Clear bit at specified position.
	 *
	 * @param position
	 *            to clear
	 */
	public void clear(int position) {
		bits.clear(position);
	}

	/**
	 * Get bit as boolean value at specified position.
	 *
	 * @param position
	 *            of bit
	 *
	 * @return returns true (bit set) or false
	 */
	public boolean get(int position) {
		return bits.get(position);
	}

	/**
	 * Returns number of bits in this list.
	 *
	 * @return int
	 */
	public int length() {
		return length;
	}

	/**
	 * Add a list of 0s to front of list.
	 *
	 * @param count
	 *            numer of bits to add at front
	 */
	public void pad(int count) {
		// new bitset of length number
		BitSet n = new BitSet(count);
		for (int i = 0; i < length; i++) {
			n.set(count + i, bits.get(i));
		}
		bits = n;
		length += count;
	}

	/**
	 * Set bit at specified position to true.
	 *
	 * @param position
	 *            start at index 0
	 */
	public void set(int position) {
		set(position, true);
	}

	/**
	 * Set bit at specified position to <code>value</code>.
	 *
	 * @param position
	 *            start at index 0
	 * @param value
	 *            value of the new bit
	 */
	public void set(int position, boolean value) {
		if (position > length) {
			length = position + 1;
		}
		bits.set(position, value);
	}

	/**
	 * return a list containing a copy of the elements starting at from, having
	 * length length.
	 *
	 * @param from
	 *            where sublist starts, list start at Index 0
	 * @param subLength
	 *            long sublist is
	 *
	 * @return LLRPBitList sublist starting at from and total length sublength
	 */

	public BitList subList(Integer from, Integer subLength) {
		if (from < 0) {
			// logger.error("try to start sublist at negative position - this is
			// not possible");
			throw new IllegalArgumentException("illegal argument: trying to start sublist at negative position");
		}

		if (length < (from + subLength)) {
			// logger.error("list not long enough. List has "+length+" elements,
			// tried to get sublist from "+from+" with length "+subLength);
			throw new IllegalArgumentException("illegal argument: from plus sublist length longer than existing list");
		}

		// return a new bitlist containing copies of the elements
		BitList b = new BitList(subLength);
		for (int i = 0; i < subLength; i++) {
			b.set(i, bits.get(from + i));
		}

		return b;
	}

	/**
	 * 8 bits bundled in one byte.
	 *
	 * @return byte Array
	 */
	public byte[] toByteArray() {
		BitBuffer buffer = BitBuffer.allocateDynamic(length);
		for (int i = 0; i < length; i++) {
			buffer.putBit(bits.get(i));
		}
		return buffer.asByteArray();
	}

	/**
	 * Encoded message as a string.
	 *
	 * @return String
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(bits.get(i) ? '1' : '0');
		}
		return sb.toString();
	}

	/**
	 * Encoded message as a HEX string.
	 *
	 * @return HEX-encoded String
	 */
	public String toHexString() {
		byte[] bytes = toByteArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Create a {@link BitList} from a HEX-encoded string representation.
	 * 
	 * @param hex
	 *            HEX-encoded string
	 * @return a new {@link BitList}F
	 */
	public static BitList fromHexString(String hex) {
		if ((hex.length() % 2) != 0) {
			hex = "0" + hex;
		}
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return new BitList(data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		BitList other = ((BitList) obj);
		if (this.length != other.length) {
			return false;
		}
		if (!Objects.equals(this.bits, other.bits)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		return Objects.hash(length, bits);
	}
}
