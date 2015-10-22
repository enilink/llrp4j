package net.enilink.llrp4j;

public class LlrpException extends RuntimeException {
	private static final long serialVersionUID = -2439950597900752285L;

	public LlrpException(final String message, Throwable cause) {
		super(message, cause);
	}

	public LlrpException(final String message) {
		super(message);
	}

	public LlrpException(Throwable e) {
		super(e);
	}
}
