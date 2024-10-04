package com.example.webtorest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

@Configuration
public class WireMockPostProcessor implements EnvironmentPostProcessor {

	private static final WireMockServer WIRE_MOCK_SERVER;

	static {
		WIRE_MOCK_SERVER = new WireMockServer(
			WireMockSpring.options()
				.asynchronousResponseEnabled(true)
				.asynchronousResponseThreads(10)
				.dynamicHttpsPort()
		);

		WIRE_MOCK_SERVER.start();

		WireMock.configureFor(new WireMock(WIRE_MOCK_SERVER));

		Runtime.getRuntime()
			.addShutdownHook(new Thread(WIRE_MOCK_SERVER::stop));
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		System.setProperty("wiremock.server.port", String.valueOf(WIRE_MOCK_SERVER.port()));
	}
}
