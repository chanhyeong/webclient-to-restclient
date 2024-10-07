package com.example.webtorest.webclient;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.webtorest.webclient.exception.CircuitRecordException;
import com.example.webtorest.webclient.exception.RemoteApiServerException;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.test.StepVerifier;

@SpringBootTest(
	properties = "spring.config.name=application-test"
)
@ActiveProfiles("test")
public class HttpBinWebApiClientTest {

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private HttpBinWebApiClient sut;

	@BeforeEach
	void setUp() {
		circuitBreakerRegistry.getAllCircuitBreakers()
			.forEach(CircuitBreaker::transitionToClosedState);
		WireMock.reset();
	}

	@Test
	void successGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(
					okJson(
						"""
							
							{
							  "origin": "211.249.71.108",
							  "url": "https://httpbin.org/get"
							}
							"""
					)
				)
		);

		// when-then
		StepVerifier.create(sut.get())
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void remoteApiExceptionGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(serverError())
		);

		// when-then
		StepVerifier.create(sut.get())
			.expectError(RemoteApiServerException.class)
			.verify();
	}

	@Test
	void circuitRecordWrappedGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(
					serviceUnavailable()
						.withHeader("Content-Type", "application/json")
				)
		);

		// when-then
		StepVerifier.create(sut.get())
			.expectErrorMatches(
				ex -> ex instanceof RemoteApiServerException
					&& ex.getCause() instanceof CircuitRecordException
			)
			.verify();
	}

	@Test
	void callNotPermittedWrappedGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(ok())
		);

		circuitBreakerRegistry.circuitBreaker("httpbin-webclient").transitionToOpenState();

		// when-then
		StepVerifier.create(sut.get())
			.expectErrorMatches(
				ex -> ex instanceof RemoteApiServerException
					&& ex.getCause() instanceof CallNotPermittedException
			)
			.verify();
	}

	@Test
	void successXml() {
		// given
		givenThat(
			get("/xml")
				.willReturn(
					okXml(
						"""
							<?xml version='1.0' encoding='us-ascii'?>
							 <!--  A SAMPLE set of slides  -->
							 <slideshow\s
							   title="Sample Slide Show"
							   date="Date of publication"
							   author="Yours Truly"
							   >
							   <!-- TITLE SLIDE -->
							   <slide type="all">
								 <title>Wake up to WonderWidgets!</title>
							   </slide>
							   <!-- OVERVIEW -->
							   <slide type="all">
								 <title>Overview</title>
								 <item>
								   Why\s
								   <em>WonderWidgets</em>
									are great
								 </item>
								 <item/>
								 <item>
								   Who\s
								   <em>buys</em>
									WonderWidgets
								 </item>
							   </slide>
							 </slideshow>
							"""
					)
				)
		);

		// when-then
		StepVerifier.create(sut.xml())
			.expectNextCount(1)
			.verifyComplete();
	}
}
