package com.example.webtorest.webclient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import reactor.test.StepVerifier;

@SpringBootTest(
	properties = "spring.config.name=application-test"
)
@ActiveProfiles({"test", "request-actual"})
public class HttpBinWebApiClientRequestActualTest {

	@Autowired
	private HttpBinWebApiClient sut;

	@Test
	void successGet() {
		// when-then
		StepVerifier.create(sut.get())
			.expectNextCount(1)
			.verifyComplete();
	}
}
