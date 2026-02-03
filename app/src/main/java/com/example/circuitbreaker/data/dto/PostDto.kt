package com.example.circuitbreaker.data.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Data class representing a blog post from JSONPlaceholder API
 */
@Serializable
data class PostDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("userId")
    val userId: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("body")
    val body: String
)