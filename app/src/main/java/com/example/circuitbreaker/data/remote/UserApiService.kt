package com.example.circuitbreaker.data.remote

import com.example.circuitbreaker.data.dto.UserDto
import retrofit2.http.GET

interface UserApiService {

    @GET("users")
    suspend fun getUsers(): List<UserDto>

}