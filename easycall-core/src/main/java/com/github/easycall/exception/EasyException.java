package com.github.easycall.exception;

public class EasyException extends Exception {
	private static final long serialVersionUID = 1L;

	public EasyException() {
		super();
	}

	public EasyException(Exception cause) {
		super(cause);
	}

	public EasyException(String message, Exception cause) {
		super(message, cause);
	}

	public EasyException(String message) {
		super(message);
	}
}
