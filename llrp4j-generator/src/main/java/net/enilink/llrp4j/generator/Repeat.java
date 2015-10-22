package net.enilink.llrp4j.generator;

public enum Repeat {
	R0_TO_1, R1, R0_TO_N, R1_TO_N;

	public static Repeat parse(String expression) {
		switch (expression) {
		case "0-1":
			return R0_TO_1;
		case "1":
			return R1;
		case "0-N":
			return R0_TO_N;
		case "1-N":
			return R1_TO_N;
		}
		throw new IllegalArgumentException("Invalid repeat expression: " + expression);
	}
}
