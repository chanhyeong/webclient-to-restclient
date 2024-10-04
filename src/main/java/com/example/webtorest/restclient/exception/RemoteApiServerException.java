package com.example.webtorest.restclient.exception;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

import lombok.SneakyThrows;

public class RemoteApiServerException extends RuntimeException {

	private final byte[] responseBody;

	private RemoteApiServerException(String message, byte[] responseBody) {
		super(message, null);
		this.responseBody = responseBody;
	}

	public static ErrorHandler of() {
		return (request, response) -> {
			throw create(response);
		};
	}

	@SneakyThrows
	static RemoteApiServerException create(ClientHttpResponse response) {
		return new RemoteApiServerException(
			"Remote api call failed",
			response.getBody().readAllBytes()
		);
	}
}
