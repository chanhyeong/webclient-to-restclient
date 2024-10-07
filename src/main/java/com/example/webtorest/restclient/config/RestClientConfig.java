package com.example.webtorest.restclient.config;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.IdleConnectionEvictor;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.DefaultThreadFactory;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestClient;

import com.example.webtorest.common.ApiClientProperties;

@Configuration
public class RestClientConfig {

	@Bean
	RestClientCustomizer restClientCustomizer() {
		return restClientBuilder ->
			restClientBuilder.defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON));
	}

	@Bean("httpBinJsonRestClient")
	RestClient httpBinJsonRestClient(
		RestClient.Builder restClientBuilder,
		@Qualifier("httpBinJsonApiClientProperties") ApiClientProperties properties
	) {
		return restClientBuilder.baseUrl(properties.getUrl())
			.defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
			.requestFactory(requestFactory(properties))
			.build();
	}

	@Bean("httpBinXmlRestClient")
	RestClient httpBinXmlRestClient(
		RestClient.Builder restClientBuilder,
		@Qualifier("httpBinXmlApiClientProperties") ApiClientProperties properties
	) {
		return restClientBuilder.baseUrl(properties.getUrl())
			.defaultHeaders(
				headers -> {
					headers.setContentType(MediaType.APPLICATION_XML);
					headers.setAccept(List.of(MediaType.APPLICATION_XML));
				}
			)
			.messageConverters(
				it -> {
					it.removeIf(converter -> converter instanceof MappingJackson2XmlHttpMessageConverter);
					it.addLast(new Jaxb2RootElementHttpMessageConverter());
				}
			)
			.requestFactory(requestFactory(properties))
			.build();
	}

	private ClientHttpRequestFactory requestFactory(ApiClientProperties properties) {
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setMaxConnPerRoute(50)
			.setDefaultConnectionConfig(
				ConnectionConfig.custom()
					.setSocketTimeout(Timeout.ofMilliseconds(properties.getReadTimeoutMillis()))
					.setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMillis()))
					.setTimeToLive(Timeout.ofMilliseconds(properties.getLifeTimeoutMillis()))
					.setValidateAfterInactivity(Timeout.ofMilliseconds(properties.getIdleTimeoutMillis()))
					.build()
			)
			.build();

		HttpClient httpClient = CustomEvictorHttpClientsBuilder.create(connectionManager, properties)
			.setRetryStrategy(AbortedExceptionRetryStrategy.INSTANCE)
			.build();

		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	private static class CustomEvictorHttpClientsBuilder extends HttpClientBuilder {
		private static final ThreadFactory EVICTOR_THREAD_FACTORY =
			new DefaultThreadFactory("idle-connection-evictor", true);

		CustomEvictorHttpClientsBuilder(
			PoolingHttpClientConnectionManager connectionManager,
			ApiClientProperties properties
		) {
			super();

			runCustomizedEvictor(connectionManager, properties);
		}

		static CustomEvictorHttpClientsBuilder create(
			PoolingHttpClientConnectionManager connectionManager,
			ApiClientProperties properties
		) {
			return new CustomEvictorHttpClientsBuilder(connectionManager, properties);
		}

		private void runCustomizedEvictor(
			PoolingHttpClientConnectionManager connectionManager,
			ApiClientProperties properties
		) {
			IdleConnectionEvictor connectionEvictor = new IdleConnectionEvictor(
				connectionManager,
				EVICTOR_THREAD_FACTORY,
				TimeValue.ofMilliseconds(properties.getIdleTimeoutMillis()),
				TimeValue.ofMilliseconds(Math.min(properties.getIdleTimeoutMillis(), 1000L))
			);

			// copy from super.build()
			super.addCloseable(() -> {
				connectionEvictor.shutdown();

				try {
					connectionEvictor.awaitTermination(Timeout.ofSeconds(1));
				} catch (final InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}
			});

			connectionEvictor.start();
		}
	}

	private static class AbortedExceptionRetryStrategy extends DefaultHttpRequestRetryStrategy {
		private static final int MAX_RETRIES = 2;

		public static AbortedExceptionRetryStrategy INSTANCE = new AbortedExceptionRetryStrategy();

		@Override
		public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
			if (super.retryRequest(request, exception, execCount, context)) {
				return true;
			}

			if (execCount > MAX_RETRIES) {
				return false;
			}

			if (exception instanceof NoHttpResponseException) {
				return true;
			}

			boolean brokenPipe = exception.getMessage().contains("Broken pipe");
			boolean connectionReset = exception.getMessage().contains("Connection reset by peer");

			return brokenPipe || connectionReset;
		}
	}
}
