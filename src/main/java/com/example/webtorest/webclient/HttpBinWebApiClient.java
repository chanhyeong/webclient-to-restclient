package com.example.webtorest.webclient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.webtorest.common.HttpBinGetResponse;
import com.example.webtorest.webclient.exception.CircuitRecordException;
import com.example.webtorest.webclient.exception.RemoteApiServerException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import reactor.core.publisher.Mono;

@Component
public class HttpBinWebApiClient {

	private final WebClient webClient;
	private final CircuitBreaker circuitBreaker;

	public HttpBinWebApiClient(
		@Qualifier("httpBinJsonWebClient") WebClient webClient,
		CircuitBreakerRegistry circuitBreakerRegistry
	) {
		this.webClient = webClient;
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("httpbin-webclient");
	}

	public Mono<HttpBinGetResponse> get() {
		return webClient.get()
			.uri("/get")
			.retrieve()
			.onStatus(
				httpStatus -> httpStatus.value() > HttpStatus.INTERNAL_SERVER_ERROR.value(),
				clientResponse -> clientResponse.createException().map(CircuitRecordException::from)
			)
			.bodyToMono(HttpBinGetResponse.class)
			.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
			.onErrorMap(RemoteApiServerException::from);
	}
}
