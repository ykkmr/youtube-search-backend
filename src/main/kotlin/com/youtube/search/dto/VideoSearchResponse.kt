package com.youtube.search.dto

data class VideoSearchResponse(
    val videos: List<VideoInfo>,
    val totalResults: Long,
    val nextPageToken: String? = null,
    val prevPageToken: String? = null
) {
    companion object {
        fun fromApiResponse(
            apiResponse: Map<String, Any>,
            request: VideoSearchRequest
        ): VideoSearchResponse {
            @Suppress("UNCHECKED_CAST")
            val items = (apiResponse["items"] as? List<Map<String, Any>>) ?: emptyList()
            
            // 검색어를 소문자로 변환하여 대소문자 구분 없이 비교
            val searchKeyword = request.keyword.lowercase().trim()
            // 검색어를 단어 단위로 분리 (공백으로 구분)
            val searchWords = searchKeyword.split("\\s+".toRegex()).filter { it.isNotBlank() }
            
            val videos = items.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                val id = (item["id"] as? Map<String, Any>)?.get("videoId") as? String
                    ?: return@mapNotNull null
                
                @Suppress("UNCHECKED_CAST")
                val snippet = item["snippet"] as? Map<String, Any> ?: return@mapNotNull null
                
                val title = snippet["title"] as? String ?: ""
                val titleLower = title.lowercase()
                
                // 제목에 검색어가 포함되어 있는지 확인 (대소문자 구분 없음)
                // 검색어가 여러 단어인 경우:
                // - 1단어: 정확히 포함되어야 함
                // - 2단어 이상: 검색어 전체가 포함되거나, 주요 단어들이 포함되어야 함
                val titleContainsKeyword = if (searchWords.size == 1) {
                    // 1단어는 정확히 포함되어야 함
                    titleLower.contains(searchWords[0])
                } else if (searchWords.size == 2) {
                    // 2단어는 모두 포함되어야 함
                    searchWords.all { word -> titleLower.contains(word) }
                } else if (searchWords.isNotEmpty()) {
                    // 3단어 이상은 검색어 전체가 포함되거나, 2/3 이상의 단어가 포함되어야 함
                    val searchKeywordInTitle = titleLower.contains(searchKeyword)
                    val matchedWords = searchWords.count { word -> titleLower.contains(word) }
                    val requiredMatches = (searchWords.size * 2 / 3.0).toInt() + 1
                    searchKeywordInTitle || matchedWords >= requiredMatches
                } else {
                    // 검색어가 없거나 공백만 있는 경우 원래 검색어 전체가 포함되어야 함
                    titleLower.contains(searchKeyword)
                }
                
                if (!titleContainsKeyword) {
                    return@mapNotNull null
                }
                
                @Suppress("UNCHECKED_CAST")
                val thumbnails = snippet["thumbnails"] as? Map<String, Any>
                @Suppress("UNCHECKED_CAST")
                val defaultThumbnail = thumbnails?.get("default") as? Map<String, Any>
                val thumbnailUrl = defaultThumbnail?.get("url") as? String ?: ""
                
                VideoInfo(
                    videoId = id,
                    title = title,
                    description = snippet["description"] as? String ?: "",
                    thumbnailUrl = thumbnailUrl,
                    channelId = snippet["channelId"] as? String ?: "",
                    channelTitle = snippet["channelTitle"] as? String ?: "",
                    publishedAt = snippet["publishedAt"] as? String ?: ""
                )
            }
            
            @Suppress("UNCHECKED_CAST")
            val pageInfo = apiResponse["pageInfo"] as? Map<String, Any>
            val totalResults = (pageInfo?.get("totalResults") as? Number)?.toLong() ?: 0L
            
            return VideoSearchResponse(
                videos = videos,
                totalResults = totalResults,
                nextPageToken = apiResponse["nextPageToken"] as? String,
                prevPageToken = apiResponse["prevPageToken"] as? String
            )
        }
    }
}

data class VideoInfo(
    val videoId: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelId: String,
    val channelTitle: String,
    val publishedAt: String,
    val viewCount: Long? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val duration: String? = null,
    val subscriberCount: Long? = null // 채널 구독자 수
)
