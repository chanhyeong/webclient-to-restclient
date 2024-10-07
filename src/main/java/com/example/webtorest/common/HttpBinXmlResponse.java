package com.example.webtorest.common;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "slideshow")
public class HttpBinXmlResponse {

	@XmlAttribute(name = "title")
	private String title;

	@XmlAttribute(name = "date")
	private String date;

	@XmlAttribute(name = "author")
	private String author;

	@XmlElement(name = "slide")
	List<SlideResponse> slides = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "slide")
	private static class SlideResponse {

		@XmlAttribute(name = "type")
		private String type;
	}
}
