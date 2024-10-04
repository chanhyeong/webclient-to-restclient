package com.example.webtorest.restclient;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
	properties = "spring.config.name=application-test"
)
@ActiveProfiles({"test", "request-actual"})
public class HttpBinRestApiClientRequestActualTest {

	@Autowired
	private HttpBinRestApiClient sut;

	@Test
	void successGet() {
		// when-then
		var actual = sut.get();

		then(actual.url()).isEqualTo("http://httpbin.org/get");
	}
}
