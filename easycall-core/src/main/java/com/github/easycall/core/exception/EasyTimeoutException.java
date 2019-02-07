package com.github.easycall.core.exception;

public class EasyTimeoutException extends Exception {
	private static final long serialVersionUID = 1L;

	public EasyTimeoutException() {
		super();
	}

	public EasyTimeoutException(Exception cause) {
		super(cause);
	}

	public EasyTimeoutException(String message, Exception cause) {
		super(message, cause);
	}

	public EasyTimeoutException(String message) {
		super(message);
	}
}
