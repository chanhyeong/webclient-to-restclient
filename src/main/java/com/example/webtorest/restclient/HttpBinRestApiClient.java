package com.example.webtorest.restclient;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.webtorest.common.GetAndXmlComposite;
import com.example.webtorest.common.HttpBinGetResponse;
import com.example.webtorest.common.HttpBinXmlResponse;
import com.example.webtorest.restclient.exception.CircuitRecordException;
import com.example.webtorest.restclient.exception.RemoteApiServerException;
import com.example.webtorest.restclient.util.ConcurrentSupports;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@Component
public class HttpBinRestApiClient {

	private final RestClient jsonRestClient;
	private final RestClient xmlRestClient;
	private final CircuitBreaker circuitBreaker;

	public HttpBinRestApiClient(
		@Qualifier("httpBinJsonRestClient") RestClient jsonRestClient,
		@Qualifier("httpBinXmlRestClient") RestClient xmlRestClient,
		CircuitBreakerRegistry circuitBreakerRegistry
	) {
		this.jsonRestClient = jsonRestClient;
		this.xmlRestClient = xmlRestClient;
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("httpbin-restclient");
	}

	public HttpBinGetResponse get() {
		return circuitBreaker.executeSupplier(() ->
			jsonRestClient.get()
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

	public HttpBinXmlResponse xml() {
		return circuitBreaker.executeSupplier(() ->
			xmlRestClient.get()
				.uri("/xml")
				.retrieve()
				.onStatus(
					httpStatus -> httpStatus.value() > HttpStatus.INTERNAL_SERVER_ERROR.value(),
					CircuitRecordException.of()
				)
				.onStatus(
					HttpStatusCode::isError,
					RemoteApiServerException.of()
				)
				.body(HttpBinXmlResponse.class)
		);
	}

	public GetAndXmlComposite composite() {
		return ConcurrentSupports.invokeAndMap(
			this::get,
			this::xml,
			(get, xml) -> GetAndXmlComposite.builder()
				.getUrl(get.url())
				.xmlTitle(xml.getTitle())
				.build()
		);
	}
}
