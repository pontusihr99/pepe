package com.pepebot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.gcp.secretmanager.enabled=false",
		"kraken.api-key=test-key",
		"kraken.secret-key=test-secret"
})
class PepeTradingBotApplicationTests {

	@Test
	void contextLoads() {
	}
}
