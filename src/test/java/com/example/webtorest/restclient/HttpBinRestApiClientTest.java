package com.example.webtorest.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okXml;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.webtorest.restclient.exception.CircuitRecordException;
import com.example.webtorest.restclient.exception.RemoteApiServerException;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(
	properties = "spring.config.name=application-test"
)
@ActiveProfiles("test")
public class HttpBinRestApiClientTest {

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private HttpBinRestApiClient sut;

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

		// when
		var actual = sut.get();

		then(actual.url()).isEqualTo("https://httpbin.org/get");
	}

	@Test
	void remoteApiExceptionGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(serverError())
		);

		// when-then
		assertThatThrownBy(sut::get)
			.isExactlyInstanceOf(RemoteApiServerException.class);
	}

	@Test
	void circuitRecordGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(
					serviceUnavailable()
						.withHeader("Content-Type", "application/json")
				)
		);

		// when-then
		assertThatThrownBy(sut::get)
			.isExactlyInstanceOf(CircuitRecordException.class);
	}

	@Test
	void callNotPermittedGet() {
		// given
		givenThat(
			get("/get")
				.willReturn(ok())
		);

		circuitBreakerRegistry.circuitBreaker("httpbin-restclient").transitionToOpenState();

		// when-then
		assertThatThrownBy(sut::get)
			.isExactlyInstanceOf(CallNotPermittedException.class);
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

		// when
		var actual = sut.xml();

		// then
		then(actual.getTitle()).isEqualTo("Sample Slide Show");
		then(actual.getSlides()).isNotEmpty();
	}
}
