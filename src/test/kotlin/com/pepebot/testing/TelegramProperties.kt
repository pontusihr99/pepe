package com.pepebot.testing

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram")
class TelegramProperties {
    var botToken: String? = null
    var chatId: String? = null
    var enabled: Boolean = true
}
