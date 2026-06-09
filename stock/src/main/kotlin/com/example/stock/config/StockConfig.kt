package com.example.stock.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class StockConfig {
    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}
