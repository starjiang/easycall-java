package com.github.easycall.exception;

public class EasyConnectException extends Exception {
	private static final long serialVersionUID = 1L;

	public EasyConnectException() {
		super();
	}

	public EasyConnectException(Exception cause) {
		super(cause);
	}

	public EasyConnectException(String message, Exception cause) {
		super(message, cause);
	}

	public EasyConnectException(String message) {
		super(message);
	}
}
