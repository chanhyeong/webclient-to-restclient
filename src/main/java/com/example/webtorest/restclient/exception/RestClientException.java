package com.example.webtorest.restclient.exception;

public abstract class RestClientException extends RuntimeException {

	protected RestClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
