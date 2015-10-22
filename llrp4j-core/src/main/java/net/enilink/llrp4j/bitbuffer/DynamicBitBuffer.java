/* BitBuffer implementation from https://github.com/magik6k/BitBuffer
 * 
 * MIT License 
 */
package net.enilink.llrp4j.bitbuffer;

class DynamicBitBuffer extends SimpleBitBuffer {
	private static final int DEFAULT_CAPACITY = 128;

	private byte[] bytes;

	protected DynamicBitBuffer() {
		bytes = new byte[DEFAULT_CAPACITY];
	}

	protected DynamicBitBuffer(int initialCapacity) {
		bytes = new byte[(int) toBytes(initialCapacity)];
	}

	private static int toBytes(int bits) {
		return (bits + (8 - bits % 8)) / 8;
	}

	@Override
	protected byte rawGet(int index) {
		if (index >= bytes.length) {
			ensureCapacity((int) index + 1);
		}
		return bytes[index];
	}

	private void ensureCapacity(int toBytes) {
		byte[] newBytes = new byte[toBytes];
		System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
		bytes = newBytes;
	}

	@Override
	protected void rawSet(int index, byte value) {
		if (index >= bytes.length) {
			ensureCapacity((int) index + 1);
		}
		bytes[index] = value;
	}

	@Override
	protected int rawLength() {
		return bytes.length * 8;
	}

	public BitBuffer slice() {
		return new ArrayBitBuffer(bytes, size() - position(), position());
	}

	public BitBuffer slice(int start, int length) {
		return new ArrayBitBuffer(bytes, Math.min(length, size() - start), start);
	}

	public BitBuffer slice(int start) {
		return slice(start, size() - start);
	}
}
