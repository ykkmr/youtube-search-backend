package com.youtube.search.controller

import com.youtube.search.dto.VideoSearchRequest
import com.youtube.search.dto.VideoSearchResponse
import com.youtube.search.service.YoutubeService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/youtube")
class YoutubeController(
    private val youtubeService: YoutubeService
) {
    
    @PostMapping("/search")
    fun searchVideos(
        @Valid @RequestBody request: VideoSearchRequest
    ): Mono<ResponseEntity<Any>> {
        return youtubeService.searchVideos(request)
            .map { ResponseEntity.ok<Any>(it) }
            .onErrorResume { error ->
                val errorMessage = error.message ?: "Unknown error occurred"
                val errorBody = mapOf(
                    "error" to "SEARCH_FAILED",
                    "message" to errorMessage
                )
                Mono.just(ResponseEntity.badRequest().body<Any>(errorBody))
            }
    }
    
    @GetMapping("/search")
    fun searchVideosGet(
        @RequestParam keyword: String,
        @RequestParam(required = false) maxResults: Int?,
        @RequestParam(required = false) order: String?,
        @RequestParam(required = false) publishedAfter: String?,
        @RequestParam(required = false) publishedBefore: String?,
        @RequestParam(required = false) videoDuration: String?,
        @RequestParam(required = false) minViewCount: Long?,
        @RequestParam(required = false) maxViewCount: Long?,
        @RequestParam(required = false) pageToken: String?
    ): Mono<ResponseEntity<Any>> {
        val request = VideoSearchRequest(
            keyword = keyword,
            maxResults = maxResults,
            order = order,
            publishedAfter = publishedAfter,
            publishedBefore = publishedBefore,
            videoDuration = videoDuration,
            pageToken = pageToken
        )
        
        return youtubeService.searchVideos(request)
            .map { ResponseEntity.ok<Any>(it) }
            .onErrorResume { error ->
                val errorMessage = error.message ?: "Unknown error occurred"
                val errorBody = mapOf(
                    "error" to "SEARCH_FAILED",
                    "message" to errorMessage
                )
                Mono.just(ResponseEntity.badRequest().body<Any>(errorBody))
            }
    }
}


