package com.github.easycall.core.exception;

public class EasyInvalidPkgException extends EasyException {
	private static final long serialVersionUID = 1L;

	public EasyInvalidPkgException() {
		super();
	}

	public EasyInvalidPkgException(Exception cause) {
		super(cause);
	}

	public EasyInvalidPkgException(String message, Exception cause) {
		super(message, cause);
	}

	public EasyInvalidPkgException(String message) {
		super(message);
	}
}
