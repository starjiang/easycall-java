package com.github.easycall.core.exception;

public class EasyServiceNotFoundException extends EasyException {
	private static final long serialVersionUID = 1L;

	public EasyServiceNotFoundException() {
		super();
	}

	public EasyServiceNotFoundException(Exception cause) {
		super(cause);
	}

	public EasyServiceNotFoundException(String message, Exception cause) {
		super(message, cause);
	}

	public EasyServiceNotFoundException(String message) {
		super(message);
	}
}
