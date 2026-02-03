package com.example.circuitbreaker.data.dto


import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

/**
 * Data class representing a user from JSONPlaceholder API
 * Used for JSON serialization/deserialization
 */
@Serializable
data class UserDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("website")
    val website: String? = null,

    @SerializedName("username")
    val username: String? = null
) {
    /**
     * Returns formatted display name for UI
     */
    fun getDisplayName(): String = "$name ($email)"
}