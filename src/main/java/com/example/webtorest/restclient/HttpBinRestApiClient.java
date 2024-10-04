package com.example.webtorest.restclient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.webtorest.common.HttpBinGetResponse;
import com.example.webtorest.restclient.exception.CircuitRecordException;
import com.example.webtorest.restclient.exception.RemoteApiServerException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Component
public class HttpBinRestApiClient {

	private final RestClient restClient;
	private final CircuitBreaker circuitBreaker;

	public HttpBinRestApiClient(
		@Qualifier("httpBinJsonRestClient") RestClient restClient,
		CircuitBreakerRegistry circuitBreakerRegistry
	) {
		this.restClient = restClient;
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("httpbin-restclient");
	}

	public HttpBinGetResponse get() {
		return circuitBreaker.executeSupplier(() ->
			restClient.get()
				.uri("/get")
				.retrieve()
				.onStatus(
					httpStatus -> httpStatus.value() > HttpStatus.INTERNAL_SERVER_ERROR.value(),
					CircuitRecordException.of()
				)
				.onStatus(
					HttpStatusCode::isError,
					RemoteApiServerException.of()
				)
				.body(HttpBinGetResponse.class)
		);
	}
}
