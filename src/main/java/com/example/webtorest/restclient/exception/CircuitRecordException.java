package com.example.webtorest.restclient.exception;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

import lombok.SneakyThrows;

public class CircuitRecordException extends RestClientException {

	private CircuitRecordException(String message, Throwable cause) {
		super(message, cause);
	}

	public static ErrorHandler of() {
		return (request, response) -> {
			throw create(response);
		};
	}

	@SneakyThrows
	static CircuitRecordException create(ClientHttpResponse response) {
		return new CircuitRecordException("Server failed", RemoteApiServerException.create(response));
	}
}
