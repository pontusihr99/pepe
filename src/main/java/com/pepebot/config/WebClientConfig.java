package com.pepebot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient krakenWebClient(WebClient.Builder builder, KrakenProperties properties) {
		return builder.baseUrl(properties.getBaseUrl()).build();
	}
}
