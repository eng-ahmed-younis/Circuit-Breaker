package com.example.circuitbreaker.data.remote

import com.example.circuitbreaker.data.dto.PostDto
import retrofit2.http.GET

interface PostApiService {

    @GET("posts")
    suspend fun getPosts(): List<PostDto>
}