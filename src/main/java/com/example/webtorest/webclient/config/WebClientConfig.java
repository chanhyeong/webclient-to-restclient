package com.example.webtorest.webclient.config;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.webtorest.common.ApiClientProperties;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {

	@Bean
	ClientRequestObservationConvention clientRequestObservationConvention() {
		return new CustomClientRequestObservation();
	}

	@Bean("httpBinJsonApiClientProperties")
	@ConfigurationProperties("api.httpbin-json")
	ApiClientProperties httpBinJsonApiClientProperties() {
		return new ApiClientProperties();
	}

	@Bean("httpBinJsonWebClient")
	WebClient httpBinJsonWebClient(
		ReactorResourceFactory reactorResourceFactory,
		WebClient.Builder webClientBuilder,
		@Qualifier("httpBinJsonApiClientProperties") ApiClientProperties properties
	) {
		return webClientBuilder.baseUrl(properties.getUrl())
			.defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
			.clientConnector(this.reactorClientHttpConnector(reactorResourceFactory, properties))
			.build();
	}

	@Bean("httpBinXmlApiClientProperties")
	@ConfigurationProperties("api.httpbin-xml")
	ApiClientProperties httpBinXmlApiClientProperties() {
		return new ApiClientProperties();
	}

	@Bean("httpBinXmlWebClient")
	WebClient httpBinXmlWebClient(
		ReactorResourceFactory reactorResourceFactory,
		WebClient.Builder webClientBuilder,
		@Qualifier("httpBinXmlApiClientProperties") ApiClientProperties properties
	) {
		return webClientBuilder.baseUrl(properties.getUrl())
			.defaultHeaders(
				headers -> {
					headers.setContentType(MediaType.APPLICATION_XML);
					headers.setAccept(List.of(MediaType.APPLICATION_XML));
				}
			)
			.clientConnector(this.reactorClientHttpConnector(reactorResourceFactory, properties))
			.build();
	}

	private ReactorClientHttpConnector reactorClientHttpConnector(
		ReactorResourceFactory reactorResourceFactory,
		ApiClientProperties properties
	) {
		return new ReactorClientHttpConnector(reactorResourceFactory, hc -> {
			ConnectionProvider connectionProvider = ConnectionProvider.builder("connection-pool")
				.maxConnections(500)
				.pendingAcquireMaxCount(1000)
				.maxIdleTime(Duration.ofMillis(properties.getIdleTimeoutMillis()))
				.maxLifeTime(Duration.ofMillis(properties.getLifeTimeoutMillis()))
				.build();

			HttpClient httpClient = HttpClient.create(connectionProvider)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMillis())
				.doOnConnected(connection ->
					connection.addHandlerFirst(
						new ReadTimeoutHandler(properties.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
					)
				);

			httpClient.warmup().block();

			return httpClient;
		});
	}

	private static class CustomClientRequestObservation extends DefaultClientRequestObservationConvention {

		@Override
		public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
			KeyValues keyValues = KeyValues.of(
				clientName(context),
				exception(context),
				method(context),
				outcome(context)
			);

			if (context.getRequest() == null) {
				return keyValues;
			}

			KeyValue customMetric = context.getRequest().attribute("custom-metric")
				.map(it -> KeyValue.of("custom-metric", it.toString()))
				.orElse(KeyValue.of("custom-metric", "unset"));

			return keyValues.and(customMetric);
		}
	}
}
