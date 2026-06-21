package com.pepebot.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kraken")
class KrakenProperties {
    var baseUrl: String = "https://api.kraken.com"
    var defaultPair: String = "PEPEEUR"
    var apiKey: String? = null
    var secretKey: String? = null
}
