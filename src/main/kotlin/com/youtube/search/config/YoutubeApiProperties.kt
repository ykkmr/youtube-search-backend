package com.youtube.search.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "youtube.api")
data class YoutubeApiProperties(
    var key: String = "",
    var baseUrl: String = "https://www.googleapis.com/youtube/v3"
)


