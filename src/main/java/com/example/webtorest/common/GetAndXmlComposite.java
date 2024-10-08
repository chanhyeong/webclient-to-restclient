package com.example.webtorest.common;

import lombok.Builder;

@Builder
public record GetAndXmlComposite(
	String getUrl,

	String xmlTitle
) {
}
