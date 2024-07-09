package com.linkedinAppReview.exception;

public class NullPointerExcep extends RuntimeException {
	String message;
	
	public NullPointerExcep(String message) {
		this.message = message;
	}
}
