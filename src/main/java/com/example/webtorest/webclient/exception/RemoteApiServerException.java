package com.example.webtorest.webclient.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class RemoteApiServerException extends RuntimeException {

	private final byte[] responseBody;

	private RemoteApiServerException(String message, Throwable cause, byte[] responseBody) {
		super(message, cause);
		this.responseBody = responseBody;
	}

	public static RemoteApiServerException from(Throwable throwable) {
		return new RemoteApiServerException(
			"Remote api call failed",
			throwable,
			throwable instanceof WebClientResponseException webClientResponseException
				? webClientResponseException.getResponseBodyAsByteArray()
				: null
		);
	}
}
