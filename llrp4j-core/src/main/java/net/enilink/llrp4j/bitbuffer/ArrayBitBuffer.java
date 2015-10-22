/* BitBuffer implementation from https://github.com/magik6k/BitBuffer
 * 
 * MIT License 
 */
package net.enilink.llrp4j.bitbuffer;

class ArrayBitBuffer extends SimpleBitBuffer {
	private byte[] bytes;

	protected ArrayBitBuffer(byte[] bytes, int limit, int offset) {
		this.bytes = bytes;
		this.limit = limit;
		this.offset = offset;
		this.size = limit;
	}

	protected ArrayBitBuffer(int bits) {
		this.bytes = new byte[(int) ((bits + (8L - bits % 8L)) / 8L)];
		this.limit = bits;
		this.size = limit;
	}

	protected ArrayBitBuffer(byte[] bytes) {
		this.bytes = bytes;
		this.limit = bytes.length * 8;
		this.size = limit;
	}

	@Override
	protected byte rawGet(int index) {
		return bytes[index];
	}

	@Override
	protected void rawSet(int index, byte value) {
		bytes[index] = value;
	}

	@Override
	protected int rawLength() {
		return limit;
	}

	@Override
	public int limit() {
		return limit;
	}

	public BitBuffer slice() {
		return new ArrayBitBuffer(bytes, size() - position(), offset + position());
	}

	public BitBuffer slice(int start, int length) {
		return new ArrayBitBuffer(bytes, Math.min(length, size() - start), offset + start);
	}

	public BitBuffer slice(int start) {
		return slice(start, size() - start);
	}
}