package com.pepebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kraken")
public class KrakenProperties {

	private String baseUrl = "https://api.kraken.com";
	private String defaultPair = "PEPEEUR";
	private String apiKey;
	private String secretKey;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getDefaultPair() {
		return defaultPair;
	}

	public void setDefaultPair(String defaultPair) {
		this.defaultPair = defaultPair;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
}
