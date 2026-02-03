package com.example.circuitbreaker.data.remote.service.ktor

import android.util.Log
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreaker
import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * API Client using Ktor with Circuit Breaker protection
 * Wraps all network calls with circuit breaker logic
 *
 * Uses JSONPlaceholder API (https://jsonplaceholder.typicode.com/) for demo
 *
 * @param client Injected Ktor HttpClient
 * @param circuitBreaker Injected CircuitBreaker instance
 */
class KtorApiClient(
    private val client: HttpClient,
    private val circuitBreaker: CircuitBreaker
) {
    companion object {
        private const val TAG = "KtorApiClient"
        private const val BASE_URL = "http://10.20.1.154:3001"
    }



    /**
     * Fetches all users from the API
     *
     * @return List of users
     * @throws CircuitBreakerOpenException if circuit is open
     * @throws Exception for other network errors
     */
    suspend fun getUsers(): List<UserDto> {
        Log.d(TAG, "Fetching users...")
        // Wrap the network call with circuit breaker
        return circuitBreaker.execute {
            // Perform GET request and deserialize response to List<User>
            val response: List<UserDto> = client.get("$BASE_URL/users").body()
            Log.d(TAG, "Successfully fetched ${response.size} users")
            response
        }
    }


    /**
     * Fetches a specific user by ID
     *
     * @param id User identifier
     * @return User object
     * @throws CircuitBreakerOpenException if circuit is open
     */
    suspend fun getUserById(id: Int): UserDto {
        Log.d(TAG, "Fetching user with id: $id")

        return circuitBreaker.execute {
            // Perform GET request with path parameter
            val response: UserDto = client.get("$BASE_URL/users/$id").body()
            Log.d(TAG, "Successfully fetched user: ${response.name}")
            response
        }
    }

    /**
     * Fetches all posts
     *
     * @return List of posts
     * @throws CircuitBreakerOpenException if circuit is open
     */
    suspend fun getPosts(): List<PostDto> {
        Log.d(TAG, "Fetching posts...")

        return circuitBreaker.execute {
            val response: List<PostDto> = client.get("$BASE_URL/posts").body()
            Log.d(TAG, "Successfully fetched ${response.size} posts")
            response
        }
    }

    /**
     * Fetches posts by user ID
     *
     * @param userId User ID to filter posts
     * @return List of posts for the specified user
     */
    suspend fun getPostsByUserId(userId: Int): List<PostDto> {
        Log.d(TAG, "Fetching posts for user: $userId")

        return circuitBreaker.execute {
            val response: List<PostDto> = client.get("$BASE_URL/posts") {
                parameter("userId", userId)
            }.body()
            Log.d(TAG, "Successfully fetched ${response.size} posts for user $userId")
            response
        }
    }

    /**
     * Returns the current circuit breaker state
     */
    fun getCircuitBreakerState() = circuitBreaker.getState()

    /**
     * Returns circuit breaker statistics
     */
    fun getCircuitBreakerStats() = circuitBreaker.getStats()


}