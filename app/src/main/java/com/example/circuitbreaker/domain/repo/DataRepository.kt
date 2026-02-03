package com.example.circuitbreaker.domain.repo

import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import com.example.circuitbreaker.domain.model.Post


interface DataRepository {
    suspend fun getUsers(): Result<List<UserDto>>
    suspend fun getPosts(): Result<List<PostDto>>
}