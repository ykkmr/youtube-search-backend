package com.youtube.search.dto

data class VideoSearchRequest(
    val keyword: String,
    val maxResults: Int? = 25,
    val order: String? = "relevance", // date, rating, relevance, title, videoCount, viewCount
    val publishedAfter: String? = null, // ISO 8601 format (e.g., 2024-01-01T00:00:00Z)
    val publishedBefore: String? = null,
    val videoDuration: String? = null, // any, short, medium, long
    val videoDefinition: String? = null, // any, high, standard
    val videoLicense: String? = null, // any, creativeCommon, youtube
    val pageToken: String? = null, // 페이지네이션 토큰
    // 필터링 옵션
    val minViewCount: Long? = null,
    val maxViewCount: Long? = null,
    val minSubscriberCount: Long? = null,
    val maxSubscriberCount: Long? = null
)


