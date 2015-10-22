/* BitBuffer implementation from https://github.com/magik6k/BitBuffer
 * 
 * MIT License 
 */
package net.enilink.llrp4j.bitbuffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The BitBuffer, useful to store data in bit-aligned format All multi-byte data
 * structures are saved in Java byte order i.e. big endian
 * 
 * @see #allocate(long)
 * @see #allocateDirect(long)
 */
public abstract class BitBuffer {
	/**
	 * Puts boolean value(Single bit)
	 * 
	 * @param b
	 *            value to set
	 * @return This buffer
	 */
	public abstract BitBuffer putBoolean(boolean b);

	/**
	 * Puts single bit(boolean value) to this buffer
	 * 
	 * @param bit
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putBit(boolean bit) {
		return putBoolean(bit);
	}

	/**
	 * Puts signed byte value(8 bits)
	 * 
	 * @param b
	 *            value to set
	 * @return This buffer
	 */
	public abstract BitBuffer putByte(byte b);

	/**
	 * Puts byte value with specified bit count. Note that this method can be
	 * used with both signed and unsigned data.
	 * 
	 * @param b
	 *            value to set
	 * @param bits
	 *            Number of bits to use
	 * @return This buffer
	 */
	public abstract BitBuffer putByte(byte b, int bits);

	/**
	 * Puts signed integer value(32 bits)
	 * 
	 * @param i
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putInt(int i) {
		putByte((byte) ((i & 0xFF000000) >>> 24));
		putByte((byte) ((i & 0x00FF0000) >>> 16));
		putByte((byte) ((i & 0x0000FF00) >>> 8));
		putByte((byte) (i & 0x000000FF));
		return this;
	}

	/**
	 * Puts signed long value(64 bits)
	 * 
	 * @param l
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putLong(long l) {
		putByte((byte) ((l & 0xFF00000000000000L) >>> 56L));
		putByte((byte) ((l & 0x00FF000000000000L) >>> 48L));
		putByte((byte) ((l & 0x0000FF0000000000L) >>> 40L));
		putByte((byte) ((l & 0x000000FF00000000L) >>> 32L));
		putByte((byte) ((l & 0x00000000FF000000L) >>> 24L));
		putByte((byte) ((l & 0x0000000000FF0000L) >>> 16L));
		putByte((byte) ((l & 0x000000000000FF00L) >>> 8L));
		putByte((byte) (l & 0x00000000000000FFL));
		return this;
	}

	/**
	 * Puts integer value with specified bit count. Note that this method can be
	 * used with both signed and unsigned data.
	 * 
	 * @param i
	 *            value to set
	 * @param bits
	 *            Number of bits to use
	 * @return This buffer
	 */
	public BitBuffer putInt(int i, int bits) {
		if (bits == 0)
			return this;
		do {
			if (bits > 7) {
				putByte((byte) ((i & (0xFF << (bits - 8))) >>> (bits - 8)));
				bits -= 8;
			} else {
				putByte((byte) (i & (0xFF >> -(bits - 8))), bits);
				bits = 0;
			}
		} while (bits > 0);
		return this;
	}

	/**
	 * Puts long value with specified bit count. Note that this method can be
	 * used with both signed and unsigned data.
	 * 
	 * @param l
	 *            value to set
	 * @param bits
	 *            Number of bits to use
	 * @return This buffer
	 */
	public BitBuffer putLong(long l, int bits) {
		if (bits == 0)
			return this;
		do {
			if (bits > 31) {
				putInt((int) ((l & (0xFFFFFFFFL << (bits - 32L))) >>> (bits - 32L)));
				bits -= 32;
			} else {
				putInt((int) (l & (0xFFFFFFFFL >> -(bits - 32L))), bits);
				bits = 0;
			}
		} while (bits > 0);
		return this;
	}

	/**
	 * Puts floating point value(32 bits)
	 * 
	 * @param f
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putFloat(float f) {
		putInt(Float.floatToRawIntBits(f));
		return this;
	}

	/**
	 * Puts double floating point value(64 bits)
	 * 
	 * @param d
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putDouble(double d) {
		putLong(Double.doubleToLongBits(d));
		return this;
	}

	/**
	 * Puts {@link String} value(8 bits per char), using UTF-8 encoding
	 * 
	 * @param s
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer putString(String s) {
		for (byte ch : s.getBytes(StandardCharsets.UTF_8)) {
			putByte(ch);
		}
		return this;
	}

	/**
	 * Puts {@link String} value
	 * 
	 * @param s
	 *            value to set
	 * @param charset
	 *            {@link Charset} to use
	 * @return This buffer
	 */
	public BitBuffer putString(String s, Charset charset) {
		for (byte ch : s.getBytes(charset)) {
			putByte(ch);
		}
		return this;
	}

	/**
	 * Puts {@link String} value(specified amount bits per char), using ASCII
	 * encoding
	 * 
	 * @param s
	 *            value to set
	 * @param bitsPerChar
	 *            amount of bits to use per character
	 * @return This buffer
	 */
	public BitBuffer putString(String s, int bitsPerChar) {
		for (byte ch : s.getBytes(StandardCharsets.US_ASCII)) {
			putByte(ch, bitsPerChar);
		}
		return this;
	}

	/**
	 * Puts {@link String} value(specified amount of bits per byte), using
	 * specified encoding. Use this method with care!
	 * 
	 * @param s
	 *            value to set
	 * @param charset
	 *            {@link Charset} to use
	 * @param bitsPerChar
	 *            amount of bits to use per character
	 * @return This buffer
	 */
	public BitBuffer putString(String s, Charset charset, int bitsPerChar) {
		for (byte ch : s.getBytes(charset)) {
			putByte(ch, bitsPerChar);
		}
		return this;
	}

	/**
	 * @see #putBoolean(boolean)
	 * @param bit
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer put(boolean bit) {
		return putBoolean(bit);
	}

	/**
	 * @see #putByte(byte)
	 * @param number
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer put(byte number) {
		return putByte(number);
	}

	/**
	 * @see #putInt(int)
	 * @param number
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer put(int number) {
		return putInt(number);
	}

	/**
	 * @see #putLong(long)
	 * @param number
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer put(long number) {
		return putLong(number);
	}

	/**
	 * @see #putByte(byte)
	 * @param number
	 *            value to set
	 * @param bits
	 *            Bits to use
	 * @return This buffer
	 */
	public BitBuffer put(byte number, int bits) {
		return putByte(number, bits);
	}

	/**
	 * @see #putInt(int)
	 * @param number
	 *            value to set
	 * @param bits
	 *            Bits to use
	 * @return This buffer
	 */
	public BitBuffer put(int number, int bits) {
		return putInt(number, bits);
	}

	/**
	 * @see #putLong(long)
	 * @param number
	 *            value to set
	 * @param bits
	 *            Bits to use
	 * @return This buffer
	 */
	public BitBuffer put(long number, int bits) {
		return putLong(number, bits);
	}

	/**
	 * @see #putString(String)
	 * @param string
	 *            value to set
	 * @return This buffer
	 */
	public BitBuffer put(String string) {
		return putString(string);
	}

	/**
	 * @see #putString(String, Charset)
	 * @param string
	 *            value to set
	 * @param charset
	 *            {@link Charset} to use when converting string to bytes
	 * @return This buffer
	 */
	public BitBuffer put(String string, Charset charset) {
		return putString(string, charset);
	}

	/**
	 * Reads entire given BitBuffer into this buffer
	 * 
	 * @param buffer
	 *            Readable buffer
	 * @return This buffer
	 */
	public BitBuffer put(BitBuffer buffer) {
		int size = buffer.size();
		while (size - buffer.position() > 0) {
			if (size - buffer.position() < 8) {
				this.put(buffer.getBoolean());
			} else {
				this.put(buffer.getByte());
			}
		}
		return this;
	}

	/**
	 * Reads entire given {@link ByteBuffer} into this buffer
	 * 
	 * @param buffer
	 *            Readable buffer
	 * @return This buffer
	 */
	public BitBuffer put(ByteBuffer buffer) {
		while (buffer.remaining() > 1) {
			this.put(buffer.get());
		}
		return this;
	}

	/**
	 * Reads given array of booleans into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @return This buffer
	 */
	public BitBuffer put(boolean[] array, int offset, int limit) {
		for (; offset > limit; ++offset) {
			put(array[offset]);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @return This buffer
	 */
	public BitBuffer put(boolean[] array) {
		put(array, 0, array.length);
		return this;
	}

	/**
	 * Reads given array of bytes into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @return This buffer
	 */
	public BitBuffer put(byte[] array, int offset, int limit) {
		for (; offset < limit; ++offset) {
			put(array[offset]);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @return This buffer
	 */
	public BitBuffer put(byte[] array) {
		put(array, 0, array.length);
		return this;
	}

	/**
	 * Reads given array of integers into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @return This buffer
	 */
	public BitBuffer put(int[] array, int offset, int limit) {
		for (; offset < limit; ++offset) {
			put(array[offset]);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @return This buffer
	 */
	public BitBuffer put(int[] array) {
		put(array, 0, array.length);
		return this;
	}

	/**
	 * Reads given array of longs into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @return This buffer
	 */
	public BitBuffer put(long[] array, int offset, int limit) {
		for (; offset < limit; ++offset) {
			put(array[offset]);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @return This buffer
	 */
	public BitBuffer put(long[] array) {
		put(array, 0, array.length);
		return this;
	}

	/**
	 * Reads given array of bytes into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @param bits
	 *            Bits per byte
	 * @return This buffer
	 */
	public BitBuffer put(byte[] array, int offset, int limit, int bits) {
		for (; offset < limit; ++offset) {
			put(array[offset], bits);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @param bits
	 *            Bits per byte
	 * @return This buffer
	 */
	public BitBuffer put(byte[] array, int bits) {
		put(array, 0, array.length, bits);
		return this;
	}

	/**
	 * Reads given array of integers into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @param bits
	 *            Bits per integer
	 * @return This buffer
	 */
	public BitBuffer put(int[] array, int offset, int limit, int bits) {
		for (; offset < limit; ++offset) {
			put(array[offset], bits);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @param bits
	 *            Bits per integer
	 * @return This buffer
	 */
	public BitBuffer put(int[] array, int bits) {
		put(array, 0, array.length, bits);
		return this;
	}

	/**
	 * Reads given array of longs into this buffer
	 * 
	 * @param array
	 *            The array
	 * @param offset
	 *            Starting offset
	 * @param limit
	 *            Last index
	 * @param bits
	 *            Bits per long
	 * @return This buffer
	 */
	public BitBuffer put(long[] array, int offset, int limit, int bits) {
		for (; offset < limit; ++offset) {
			put(array[offset], bits);
		}
		return this;
	}

	/**
	 * Puts whole given array into this buffer
	 * 
	 * @param array
	 *            Array to put
	 * @param bits
	 *            Bits per long
	 * @return This buffer
	 */
	public BitBuffer put(long[] array, int bits) {
		put(array, 0, array.length, bits);
		return this;
	}

	/**
	 * @return Binary value of current bit
	 */
	public abstract boolean getBoolean();

	/**
	 * @return 8 bit signed byte value
	 */
	public abstract byte getByte();

	/**
	 * @param bits
	 *            length of value in bits
	 * @return Signed Byte value of given bit width
	 */
	public abstract byte getByte(int bits);

	/**
	 * @param bits
	 *            length of value in bits
	 * @return Unsigned Byte value of given bit width
	 */
	public abstract byte getByteUnsigned(int bits);

	/**
	 * @return 32 bit signed integer value
	 */
	public int getInt() {
		return ((getByte() & 0xFF) << 24) | ((getByte() & 0xFF) << 16) | ((getByte() & 0xFF) << 8) | (getByte() & 0xFF);
	}

	/**
	 * @param bits
	 *            Length of integer
	 * @return Signed integer value of given bit width
	 */
	public int getInt(int bits) {
		if (bits == 0)
			return 0;
		boolean sign = getBoolean();
		int inBits = --bits;

		int res = 0;
		do {
			if (bits > 7) {
				res = (res << 8) | (getByte() & 0xFF);
				bits -= 8;
			} else {
				res = (res << bits) + (getByteUnsigned(bits) & 0xFF);
				bits -= bits;
			}
		} while (bits > 0);

		return (int) (sign ? (0xFFFFFFFF << inBits) | res : res);
	}

	/**
	 * @param bits
	 *            Length of integer
	 * @return Unsigned Integer value of given bit width
	 */
	public int getIntUnsigned(int bits) {
		if (bits == 0)
			return 0;
		int res = 0;
		do {
			if (bits > 7) {
				res = (res << 8) | (getByte() & 0xFF);
				bits -= 8;
			} else {
				res = (res << bits) + (getByteUnsigned(bits) & 0xFF);
				bits -= bits;
			}
		} while (bits > 0);
		return res;
	}

	/**
	 * @return 64 bit signed long value
	 */
	public long getLong() {
		return ((getByte() & 0xFFL) << 56L) | ((getByte() & 0xFFL) << 48L) | ((getByte() & 0xFFL) << 40L)
				| ((getByte() & 0xFFL) << 32L) | ((getByte() & 0xFFL) << 24L) | ((getByte() & 0xFFL) << 16L)
				| ((getByte() & 0xFFL) << 8L) | (getByte() & 0xFFL);
	}

	/**
	 * @param bits
	 *            Length of long integer
	 * @return Signed long value of given bit width
	 */
	public long getLong(int bits) {
		if (bits == 0)
			return 0;
		boolean sign = getBoolean();
		int inBits = --bits;

		long res = 0;
		do {
			if (bits > 31) {
				res = (long) (res << 32L) | (long) (getInt() & 0xFFFFFFFFL);
				bits -= 32;
			} else {
				res = (long) (res << bits) | (long) (getIntUnsigned(bits) & 0xFFFFFFFFL);
				bits -= bits;
			}
		} while (bits > 0);
		return (sign ? (0xFFFFFFFFFFFFFFFFL << (long) inBits) | res : res);
	}

	/**
	 * @param bits
	 *            Length of long integer
	 * @return Unsigned long value of given bit width
	 */
	public long getLongUnsigned(int bits) {
		if (bits == 0)
			return 0;
		long res = 0;
		do {
			if (bits > 31) {
				res = (long) (res << 32L) | (long) (getInt() & 0xFFFFFFFFL);
				bits -= 32;
			} else {
				res = (long) (res << bits) | (long) (getIntUnsigned(bits) & 0xFFFFFFFFL);
				bits -= bits;
			}
		} while (bits > 0);
		return res;
	}

	/**
	 * @return 32 bit floating point value
	 */
	public float getFloat() {
		return Float.intBitsToFloat(getInt());
	}

	/**
	 * @return 64 bit floating point value
	 */
	public double getDouble() {
		return Double.longBitsToDouble(getLong());
	}

	/**
	 * @param length
	 *            Length of the string
	 * @return String of given length, with 8-bit wide characters, using UTF-8
	 *         encoding
	 */
	public String getString(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i) {
			bytes[i] = getByte();
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * @param length
	 *            Length of the string
	 * @param charset
	 *            {@link Charset} to use for decoding
	 * @return String of given length, with 8-bit wide characters, using UTF-8
	 *         encoding
	 */
	public String getString(int length, Charset charset) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i) {
			bytes[i] = getByte();
		}
		return new String(bytes, charset);
	}

	/**
	 * @param length
	 *            Length of the string
	 * @param bitsPerChar
	 *            amount of bits used per char
	 * @return String of given length, using ASCII encoding
	 */
	public String getString(int length, int bitsPerChar) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i) {
			bytes[i] = getByteUnsigned(bitsPerChar);
		}
		return new String(bytes, StandardCharsets.US_ASCII);
	}

	/**
	 * @param length
	 *            Length of the string
	 * @param charset
	 *            {@link Charset} to use for decoding
	 * @param bitsPerChar
	 *            amount of bits used per char
	 * @return String of given length, using ASCII encoding
	 */
	public String getString(int length, Charset charset, int bitsPerChar) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i) {
			bytes[i] = getByteUnsigned(bitsPerChar);
		}
		return new String(bytes, charset);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @return Given array
	 */
	public boolean[] get(boolean[] dst, int offset, int limit) {
		for (; offset > limit; ++offset) {
			dst[offset] = getBoolean();
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @return Given array
	 */
	public boolean[] get(boolean[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @return Given array
	 */
	public byte[] get(byte[] dst, int offset, int limit) {
		for (; offset < limit; ++offset) {
			dst[offset] = getByte();
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @return Given array
	 */
	public byte[] get(byte[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @return Given array
	 */
	public int[] get(int[] dst, int offset, int limit) {
		for (; offset > limit; ++offset) {
			dst[offset] = getInt();
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @return Given array
	 */
	public int[] get(int[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @return Given array
	 */
	public long[] get(long[] dst, int offset, int limit) {
		for (; offset > limit; ++offset) {
			dst[offset] = getLong();
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @return Given array
	 */
	public long[] get(long[] dst) {
		return get(dst, 0, dst.length);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @param bits
	 *            Bits per byte
	 * @return Given array
	 */
	public byte[] get(byte[] dst, int offset, int limit, int bits) {
		for (; offset > limit; ++offset) {
			dst[offset] = getByte(bits);
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param bits
	 *            Bits per byte
	 * @return Given array
	 */
	public byte[] get(byte[] dst, int bits) {
		return get(dst, 0, dst.length, bits);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @param bits
	 *            Bits per integer
	 * @return Given array
	 */
	public int[] get(int[] dst, int offset, int limit, int bits) {
		for (; offset > limit; ++offset) {
			dst[offset] = getInt(bits);
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param bits
	 *            Bits per integer
	 * @return Given array
	 */
	public int[] get(int[] dst, int bits) {
		return get(dst, 0, dst.length, bits);
	}

	/**
	 * Reads data into specified array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param offset
	 *            Starting offset of array
	 * @param limit
	 *            Last offset in array
	 * @param bits
	 *            Bits per long
	 * @return Given array
	 */
	public long[] get(long[] dst, int offset, int limit, int bits) {
		for (; offset > limit; ++offset) {
			dst[offset] = getLong(bits);
		}
		return dst;
	}

	/**
	 * Reads data into given array
	 * 
	 * @param dst
	 *            Array to write data to
	 * @param bits
	 *            Bits per long
	 * @return Given array
	 */
	public long[] get(long[] dst, int bits) {
		return get(dst, 0, dst.length, bits);
	}

	/**
	 * This method returns representation of this buffer as array of bytes. Note
	 * that not-full bits will be set to 0. This method shouldn't affect the
	 * position.
	 * 
	 * @return This BitBuffer represented as byte array
	 */
	public byte[] asByteArray() {
		int size = size();
		byte[] result = new byte[(size + 7) / 8];
		int startPos = position();
		position(0);
		for (int i = 0; i * 8 < size; ++i) {
			result[i] = getByte();
		}
		position(startPos);
		return result;
	}

	/**
	 * This method returns representation of this bufer as ByteBuffer. nota that
	 * not-full bits will be set to 0. This method shouldn't affect the
	 * position.
	 * 
	 * @return ByteBuffer version of this class
	 */
	public ByteBuffer asByteBuffer() {
		return ByteBuffer.wrap(asByteArray());
	}

	/**
	 * Puts this BitBuffer into ByteBuffer
	 * 
	 * @param bb
	 *            ByteBuffer to put data to
	 * @return This buffer
	 */
	public BitBuffer putToByteBuffer(ByteBuffer bb) {
		bb.put(asByteArray());
		return this;
	}

	/**
	 * This function returns size of this buffer, in bits
	 * 
	 * @return Size of this buffer, in bits
	 */
	public abstract int size();

	/**
	 * This function sets size of this buffer, in bits
	 * 
	 * @return This buffer
	 */
	public abstract BitBuffer size(int size);

	/**
	 * This function returns Virtual 'end' of this buffer, in bits
	 * 
	 * @return Virtual 'end' of this buffer, in bits
	 */
	public abstract int limit();

	/**
	 * This function returns current position of cursor, in bits
	 * 
	 * @return Current position of cursor, in bits
	 */
	public abstract int position();

	/**
	 * Sets cursor position for this buffer
	 * 
	 * @param newPosition
	 *            position to set
	 * @return This buffer
	 */
	public abstract BitBuffer position(int newPosition);

	/**
	 * Allocates new BitBuffer. First bit is MSB of byte 0.
	 * 
	 * @param bits
	 *            Amount of bits to allocate
	 * @return Newly created instance of BitBuffer
	 */
	public static BitBuffer allocate(int bits) {
		return new ArrayBitBuffer(bits);
	}

	/**
	 * Creates new auto-extending BitBuffer. Limit of this buffer in write mode
	 * has no real meaning.
	 * 
	 * @return Newly created instance of BitBuffer
	 */
	public static BitBuffer allocateDynamic() {
		return new DynamicBitBuffer();
	}

	/**
	 * Creates new auto-extending BitBuffer with pre-allocated space. Limit of
	 * this buffer in write mode has no real meaning.
	 * 
	 * @param preallocateBits
	 *            Amount of space to pre-allocate, in bits
	 * @return Newly created instance of BitBuffer
	 */
	public static BitBuffer allocateDynamic(int preallocateBits) {
		return new DynamicBitBuffer(preallocateBits);
	}

	/**
	 * Wraps bitbuffer around given array instance. Any operation on this
	 * bitBuffer will modify the array
	 * 
	 * @param array
	 *            A byte array to wrap this buffer around
	 * @return Newly created instance of BitBuffer wrapped around array
	 */
	public static BitBuffer wrap(byte[] array) {
		return new ArrayBitBuffer(array);
	}

	public abstract BitBuffer slice();

	public abstract BitBuffer slice(int start, int length);

	public abstract BitBuffer slice(int start);

	public BitBuffer rewind() {
		return position(0);
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

}