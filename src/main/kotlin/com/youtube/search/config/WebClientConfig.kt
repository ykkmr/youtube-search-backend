package com.youtube.search.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    private val youtubeApiProperties: YoutubeApiProperties
) {
    
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl(youtubeApiProperties.baseUrl)
            .build()
    }
}


