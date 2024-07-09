package com.linkedinAppReview.exception;

public class CommonException extends RuntimeException {
	String message;

	public CommonException(String message) {

		this.message = message;
	}
}
