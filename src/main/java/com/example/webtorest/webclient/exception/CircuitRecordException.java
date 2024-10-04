package com.example.webtorest.webclient.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class CircuitRecordException extends WebClientResponseException {

	private CircuitRecordException(WebClientResponseException exception) {
		super(
			exception.getMessage(),
			exception.getStatusCode().value(),
			exception.getStatusText(),
			exception.getHeaders(),
			exception.getResponseBodyAsByteArray(),
			exception.getHeaders().getContentType().getCharset(), // remain npe possible because example code
			exception.getRequest()
		);
	}

	public static CircuitRecordException from(WebClientResponseException exception) {
		return new CircuitRecordException(exception);
	}
}
