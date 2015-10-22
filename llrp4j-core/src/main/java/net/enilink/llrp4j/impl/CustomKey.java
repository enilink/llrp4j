package net.enilink.llrp4j.impl;

public class CustomKey {
	public final long vendor;
	public final long subType;

	public CustomKey(long vendor, long subType) {
		this.vendor = vendor;
		this.subType = subType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (subType ^ (subType >>> 32));
		result = prime * result + (int) (vendor ^ (vendor >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof CustomKey))
			return false;
		CustomKey other = (CustomKey) obj;
		if (subType != other.subType)
			return false;
		if (vendor != other.vendor)
			return false;
		return true;
	}

}