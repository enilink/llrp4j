/* BitBuffer implementation from https://github.com/magik6k/BitBuffer
 * 
 * MIT License 
 */
package net.enilink.llrp4j.bitbuffer;

public abstract class SimpleBitBuffer extends BitBuffer {
	private int position;
	protected int offset;
	protected int limit;
	protected int size;

	protected abstract byte rawGet(int index);

	protected abstract void rawSet(int index, byte value);

	protected abstract int rawLength();

	protected void advance(int p, boolean write) {
		position += p;
		if (write && position > size) {
			size = position;
		}
	}

	@Override
	public BitBuffer putBoolean(boolean b) {
		int pos = offset + position;
		advance(1, true);
		rawSet(pos / 8, (byte) ((rawGet(pos / 8) & ~(0x80 >>> (pos % 8))) + ((b ? 0x80 : 0) >>> (pos % 8))));
		return this;
	}

	@Override
	public BitBuffer putByte(byte b) {
		int pos = offset + position;
		advance(8, true);
		byte old = (byte) (rawGet(pos / 8) & (byte) ~(0xFF >>> (pos % 8)));
		rawSet(pos / 8, (byte) (old | (byte) ((b & 0xFF) >>> (pos % 8))));
		if (pos % 8 > 0)
			rawSet((pos / 8) + 1, (byte) ((b & 0xFF) << (8 - (pos % 8))));
		return this;
	}

	@Override
	public BitBuffer putByte(byte b, int bits) {
		int pos = offset + position;
		advance(bits, true);
		b = (byte) (0xFF & ((b & (0xFF >>> (8 - bits))) << (8 - bits)));
		rawSet(pos / 8, (byte) (0xFF & ((rawGet(pos / 8) & (0xFF << (8 - pos % 8))) | ((b & 0xFF) >>> (pos % 8)))));
		if (8 - (pos % 8) < bits)
			rawSet((pos / 8) + 1, (byte) (0xFF & ((b & 0xFF) << (8 - pos % 8))));
		return this;
	}

	@Override
	public boolean getBoolean() {
		int pos = offset + position;
		advance(1, false);
		boolean result = (rawGet(pos / 8) & (0x80 >>> (pos % 8))) > 0;
		return result;
	}

	@Override
	public byte getByte() {
		int pos = offset + position;
		advance(8, false);
		byte b = (byte) ((rawGet(pos / 8) & (0xFF >>> (pos % 8))) << (pos % 8));
		b = pos % 8 > 0 ? (byte) (b | (((0xFF & rawGet((pos / 8) + 1)) >>> (8 - (pos % 8))))) : b;
		return b;
	}

	@Override
	public byte getByte(int bits) {
		int pos = offset + position;
		advance(bits, false);
		boolean sign = (rawGet(pos / 8) & (0x80 >>> (pos % 8))) > 0;
		pos++;
		bits--;

		short mask = (short) (((0xFF00 << (8 - bits)) & 0xFFFF) >>> (pos % 8));

		byte b = (byte) ((rawGet(pos / 8) & ((mask & 0xFF00) >>> 8)) << (pos % 8));
		if (8 - (pos % 8) < bits)
			b = (byte) (b | ((0xFF & (rawGet((pos / 8) + 1) & (mask & 0x00FF))) >>> (bits - ((pos % 8) + bits - 8))));

		b = (byte) ((b & 0xFF) >>> (8 - bits));
		return (byte) (sign ? ((0xFF << bits) & 0xFF) | b : b);
	}

	@Override
	public byte getByteUnsigned(int bits) {
		int pos = offset + position;
		advance(bits, false);
		short mask = (short) (((0xFF00 << (8 - bits)) & 0xFFFF) >>> (pos % 8));

		byte b = (byte) ((rawGet(pos / 8) & ((mask & 0xFF00) >>> 8)) << (pos % 8));
		if (8 - (pos % 8) < bits)
			b = (byte) (b | ((0xFF & (rawGet((pos / 8) + 1) & (mask & 0x00FF))) >>> (bits - ((pos % 8) + bits - 8))));

		b = (byte) ((b & 0xFF) >>> (8 - bits));
		return b;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public BitBuffer size(int size) {
		this.size = size;
		return this;
	}

	@Override
	public int limit() {
		return rawLength();
	}

	@Override
	public int position() {
		return position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + size();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof BitBuffer))
			return false;

		BitBuffer other = (BitBuffer) obj;
		int size = size();
		if (size() != other.size()) {
			return false;
		}
		int mark = position();
		int otherMark = other.position();
		try {
			for (int i = 0; i < size;) {
				if (size - i > 7) {
					byte a = getByte();
					byte b = other.getByte();
					if (a != b) {
						return false;
					}
					i += 7;
				} else {
					i += 1;
					if (getBoolean() != other.getBoolean()) {
						return false;
					}
				}
			}
		} finally {
			position(mark);
			other.position(otherMark);
		}
		return true;
	}

	@Override
	public BitBuffer position(int newPosition) {
		position = newPosition;
		return this;
	}
	
	public int offset() {
		return offset;
	}
}
