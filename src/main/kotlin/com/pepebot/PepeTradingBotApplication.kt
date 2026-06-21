package com.pepebot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class PepeTradingBotApplication

fun main(args: Array<String>) {
    runApplication<PepeTradingBotApplication>(*args)
}
