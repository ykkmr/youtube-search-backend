package com.youtube.search.service

import com.youtube.search.config.YoutubeApiProperties
import com.youtube.search.dto.VideoSearchRequest
import com.youtube.search.dto.VideoSearchResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class YoutubeService(
    private val youtubeApiProperties: YoutubeApiProperties,
    private val webClient: WebClient
) {
    
    fun searchVideos(request: VideoSearchRequest): Mono<VideoSearchResponse> {
        // API 키 확인
        if (youtubeApiProperties.key.isBlank() || youtubeApiProperties.key == "your-youtube-api-key-here") {
            return Mono.error(RuntimeException("YouTube API key is not configured. Please set YOUTUBE_API_KEY environment variable or configure it in application.yml"))
        }
        
        // 조회수 필터링이 필요한 경우 더 많은 결과를 가져와야 함
        // 제목 필터링도 추가되므로 더 많은 결과를 가져와야 함
        // shorts (1분 미만) 필터링도 영상 상세 정보가 필요하므로 필터링 필요
        val isShortsFilter = request.videoDuration == "shorts"
        val needsFiltering = request.minViewCount != null || request.maxViewCount != null || 
                           request.minSubscriberCount != null || request.maxSubscriberCount != null ||
                           isShortsFilter
        
        val searchResults = request.maxResults ?: 25
        // 제목 필터링을 위해 더 많은 결과를 가져옴 (최대 50개)
        // 제목 필터링이 적용되므로 더 많은 결과를 가져와야 함
        // shorts 필터링의 경우 더 많은 결과가 필요 (1분 미만은 적을 수 있음)
        val fetchCount = if (needsFiltering) {
            if (isShortsFilter) minOf(searchResults * 10, 50) else minOf(searchResults * 5, 50)
        } else {
            minOf(searchResults * 3, 50)
        }
        
        // shorts인 경우 short로 변환하여 YouTube API에 요청
        val apiVideoDuration = if (request.videoDuration == "shorts") "short" else request.videoDuration
        
        val searchResponse = webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("q", request.keyword)
                    .queryParam("type", "video")
                    .queryParam("maxResults", fetchCount)
                    .queryParam("order", request.order ?: "relevance")
                    .queryParam("key", youtubeApiProperties.key)
                    .apply {
                        request.publishedAfter?.let { queryParam("publishedAfter", it) }
                        request.publishedBefore?.let { queryParam("publishedBefore", it) }
                        apiVideoDuration?.let { queryParam("videoDuration", it) }
                        request.videoDefinition?.let { queryParam("videoDefinition", it) }
                        request.videoLicense?.let { queryParam("videoLicense", it) }
                        request.pageToken?.let { queryParam("pageToken", it) }
                    }
                    .build()
            }
            .retrieve()
            .onStatus({ it.isError }) { response ->
                response.bodyToMono<String>()
                    .defaultIfEmpty("Unknown error")
                    .flatMap { body ->
                        Mono.error<Throwable>(
                            RuntimeException("YouTube API error (${response.statusCode()}): $body")
                        )
                    }
            }
            .bodyToMono<Map<String, Any>>()
            .timeout(Duration.ofSeconds(10))
        
        return if (needsFiltering) {
            searchResponse.flatMap { response ->
                enrichAndFilterVideos(response, request, searchResults)
            }
        } else {
            searchResponse.map { response ->
                VideoSearchResponse.fromApiResponse(response, request)
            }
        }
    }
    
    private fun enrichAndFilterVideos(
        searchResponse: Map<String, Any>,
        request: VideoSearchRequest,
        maxResults: Int
    ): Mono<VideoSearchResponse> {
        @Suppress("UNCHECKED_CAST")
        val items = (searchResponse["items"] as? List<Map<String, Any>>) ?: emptyList()
        
        if (items.isEmpty()) {
            return Mono.just(VideoSearchResponse.fromApiResponse(searchResponse, request))
        }
        
        val videoIds = items.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            (item["id"] as? Map<String, Any>)?.get("videoId") as? String
        }
        
        val channelIds = items.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            val snippet = item["snippet"] as? Map<String, Any>
            snippet?.get("channelId") as? String
        }.distinct()
        
        // 영상 상세 정보와 채널 정보를 병렬로 가져오기
        val videoDetailsMono = if (videoIds.isNotEmpty()) {
            getVideoDetails(videoIds)
        } else {
            Mono.just(emptyMap<String, Any>())
        }
        
        val channelDetailsMono = if (channelIds.isNotEmpty()) {
            getChannelDetailsBatch(channelIds)
        } else {
            Mono.just(emptyMap<String, Any>())
        }
        
        return Mono.zip(videoDetailsMono, channelDetailsMono) { videoDetailsResult, channelDetailsResult ->
            @Suppress("UNCHECKED_CAST")
            val videoDetails = videoDetailsResult as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val channelDetails = channelDetailsResult as? Map<String, Any> ?: emptyMap()
            
            val filteredVideos = filterVideos(
                items,
                videoDetails,
                channelDetails,
                request,
                maxResults
            )
            
            @Suppress("UNCHECKED_CAST")
            val pageInfo = searchResponse["pageInfo"] as? Map<String, Any>
            val totalResults = (pageInfo?.get("totalResults") as? Number)?.toLong() ?: 0L
            
            VideoSearchResponse(
                videos = filteredVideos,
                totalResults = totalResults,
                nextPageToken = searchResponse["nextPageToken"] as? String,
                prevPageToken = searchResponse["prevPageToken"] as? String
            )
        }
    }
    
    private fun getChannelDetailsBatch(channelIds: List<String>): Mono<Map<String, Any>> {
        // YouTube API는 최대 50개까지 한 번에 조회 가능
        val batches = channelIds.chunked(50)
        val monos = batches.map { batch ->
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/channels")
                        .queryParam("part", "snippet,statistics")
                        .queryParam("id", batch.joinToString(","))
                        .queryParam("key", youtubeApiProperties.key)
                        .build()
                }
                .retrieve()
                .onStatus({ it.isError }) { response ->
                    response.bodyToMono<String>()
                        .defaultIfEmpty("Unknown error")
                        .flatMap { body ->
                            Mono.error<Throwable>(
                                RuntimeException("YouTube API error (${response.statusCode()}): $body")
                            )
                        }
                }
                .bodyToMono<Map<String, Any>>()
                .timeout(Duration.ofSeconds(10))
        }
        
        return Mono.zip(monos) { results ->
            val combined = mutableMapOf<String, Any>()
            @Suppress("UNCHECKED_CAST")
            results.forEach { result ->
                val resultMap = result as? Map<String, Any> ?: return@forEach
                @Suppress("UNCHECKED_CAST")
                val items = (resultMap["items"] as? List<Map<String, Any>>) ?: emptyList()
                items.forEach { item ->
                    @Suppress("UNCHECKED_CAST")
                    val id = item["id"] as? String ?: return@forEach
                    combined[id] = item
                }
            }
            combined
        }
    }
    
    private fun filterVideos(
        items: List<Map<String, Any>>,
        videoDetails: Map<String, Any>,
        channelDetails: Map<String, Any>,
        request: VideoSearchRequest,
        maxResults: Int
    ): List<com.youtube.search.dto.VideoInfo> {
        @Suppress("UNCHECKED_CAST")
        val videoDetailsItems = (videoDetails["items"] as? List<Map<String, Any>>) ?: emptyList()
        
        val videoStatsMap = videoDetailsItems.associate { video ->
            @Suppress("UNCHECKED_CAST")
            val id = video["id"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val statistics = video["statistics"] as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val contentDetails = video["contentDetails"] as? Map<String, Any> ?: emptyMap()
            
            id to mapOf(
                "viewCount" to ((statistics["viewCount"] as? String)?.toLongOrNull() ?: 0L),
                "likeCount" to ((statistics["likeCount"] as? String)?.toLongOrNull() ?: 0L),
                "commentCount" to ((statistics["commentCount"] as? String)?.toLongOrNull() ?: 0L),
                "duration" to (contentDetails["duration"] as? String)
            )
        }
        
        @Suppress("UNCHECKED_CAST")
        val channelStatsMap = channelDetails.entries.associate { (id, data) ->
            @Suppress("UNCHECKED_CAST")
            val channelData = data as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val statistics = channelData["statistics"] as? Map<String, Any> ?: emptyMap()
            id to ((statistics["subscriberCount"] as? String)?.toLongOrNull() ?: 0L)
        }
        
        // 검색어를 소문자로 변환하여 대소문자 구분 없이 비교
        val searchKeyword = request.keyword.lowercase().trim()
        // 검색어를 단어 단위로 분리 (공백으로 구분)
        val searchWords = searchKeyword.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        val filtered = items.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            val id = (item["id"] as? Map<String, Any>)?.get("videoId") as? String ?: return@mapNotNull null
            
            @Suppress("UNCHECKED_CAST")
            val snippet = item["snippet"] as? Map<String, Any> ?: return@mapNotNull null
            
            val title = snippet["title"] as? String ?: ""
            val titleLower = title.lowercase()
            
            // 제목에 검색어의 모든 단어가 포함되어 있는지 확인 (대소문자 구분 없음)
            // 검색어가 여러 단어인 경우, 모든 단어가 제목에 포함되어야 함
            val titleContainsAllWords = if (searchWords.isNotEmpty()) {
                searchWords.all { word -> titleLower.contains(word) }
            } else {
                // 검색어가 없거나 공백만 있는 경우 원래 검색어 전체가 포함되어야 함
                titleLower.contains(searchKeyword)
            }
            
            if (!titleContainsAllWords) {
                return@mapNotNull null
            }
            
            @Suppress("UNCHECKED_CAST")
            val channelId = snippet["channelId"] as? String ?: ""
            
            val stats = videoStatsMap[id] ?: emptyMap()
            val viewCount = stats["viewCount"] as? Long ?: 0L
            val subscriberCount = channelStatsMap[channelId] ?: 0L
            val duration = stats["duration"] as? String
            
            // shorts 필터링: 1분 미만인지 확인
            if (request.videoDuration == "shorts") {
                val durationInSeconds = parseDurationToSeconds(duration)
                if (durationInSeconds == null || durationInSeconds >= 60) {
                    return@mapNotNull null
                }
            }
            
            // 필터링 조건 확인
            if (request.minViewCount != null && viewCount < request.minViewCount) return@mapNotNull null
            if (request.maxViewCount != null && viewCount > request.maxViewCount) return@mapNotNull null
            if (request.minSubscriberCount != null && subscriberCount < request.minSubscriberCount) return@mapNotNull null
            if (request.maxSubscriberCount != null && subscriberCount > request.maxSubscriberCount) return@mapNotNull null
            
            @Suppress("UNCHECKED_CAST")
            val thumbnails = snippet["thumbnails"] as? Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val defaultThumbnail = thumbnails?.get("default") as? Map<String, Any>
            val thumbnailUrl = defaultThumbnail?.get("url") as? String ?: ""
            
            com.youtube.search.dto.VideoInfo(
                videoId = id,
                title = title,
                description = snippet["description"] as? String ?: "",
                thumbnailUrl = thumbnailUrl,
                channelId = channelId,
                channelTitle = snippet["channelTitle"] as? String ?: "",
                publishedAt = snippet["publishedAt"] as? String ?: "",
                viewCount = viewCount,
                likeCount = stats["likeCount"] as? Long,
                commentCount = stats["commentCount"] as? Long,
                duration = stats["duration"] as? String,
                subscriberCount = subscriberCount
            )
        }
        
        return filtered.take(maxResults)
    }
    
    /**
     * YouTube API의 duration 형식 (ISO 8601, 예: PT1M30S)을 초 단위로 변환
     * PT1M30S = 1분 30초 = 90초
     */
    private fun parseDurationToSeconds(duration: String?): Int? {
        if (duration == null || duration.isBlank()) return null
        
        try {
            // PT1H2M30S 형식 파싱
            var totalSeconds = 0
            var currentNumber = ""
            
            for (char in duration) {
                when (char) {
                    'P', 'T' -> continue // 시작 문자 무시
                    'H' -> {
                        totalSeconds += (currentNumber.toIntOrNull() ?: 0) * 3600
                        currentNumber = ""
                    }
                    'M' -> {
                        totalSeconds += (currentNumber.toIntOrNull() ?: 0) * 60
                        currentNumber = ""
                    }
                    'S' -> {
                        totalSeconds += currentNumber.toIntOrNull() ?: 0
                        currentNumber = ""
                    }
                    in '0'..'9' -> currentNumber += char
                }
            }
            
            return totalSeconds
        } catch (e: Exception) {
            return null
        }
    }
    
    fun getVideoDetails(videoIds: List<String>): Mono<Map<String, Any>> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/videos")
                    .queryParam("part", "snippet,statistics,contentDetails")
                    .queryParam("id", videoIds.joinToString(","))
                    .queryParam("key", youtubeApiProperties.key)
                    .build()
            }
            .retrieve()
            .onStatus({ it.isError }) { response ->
                response.bodyToMono<String>()
                    .defaultIfEmpty("Unknown error")
                    .flatMap { body ->
                        Mono.error<Throwable>(
                            RuntimeException("YouTube API error (${response.statusCode()}): $body")
                        )
                    }
            }
            .bodyToMono<Map<String, Any>>()
            .timeout(Duration.ofSeconds(10))
    }
    
    fun getChannelDetails(channelId: String): Mono<Map<String, Any>> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/channels")
                    .queryParam("part", "snippet,statistics")
                    .queryParam("id", channelId)
                    .queryParam("key", youtubeApiProperties.key)
                    .build()
            }
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .timeout(Duration.ofSeconds(10))
    }
}

