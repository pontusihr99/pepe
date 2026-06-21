package com.pepebot.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration(proxyBeanMethods = false)
class WebClientConfig {
    @Bean
    fun krakenWebClient(builder: WebClient.Builder, properties: KrakenProperties): WebClient {
        return builder.baseUrl(properties.baseUrl).build()
    }
}
