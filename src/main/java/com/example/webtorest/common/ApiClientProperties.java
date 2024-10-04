package com.example.webtorest.common;

import lombok.Data;

@Data
public class ApiClientProperties {

	private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 2_000;
	private static final int DEFAULT_READ_TIMEOUT_MILLIS = 2_000;

	private static final int DEFAULT_IDLE_TIMEOUT_MILLIS = 1_000;
	private static final int DEFAULT_LIFE_TIMEOUT_MILLIS = 60_000;

	private String url;

	private int connectTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;

	private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

	private int idleTimeoutMillis = DEFAULT_IDLE_TIMEOUT_MILLIS;

	private int lifeTimeoutMillis = DEFAULT_LIFE_TIMEOUT_MILLIS;
}
