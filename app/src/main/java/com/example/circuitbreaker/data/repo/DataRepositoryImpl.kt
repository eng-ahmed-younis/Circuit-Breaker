package com.example.circuitbreaker.repository

import android.util.Log
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreakerOpenException
import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import com.example.circuitbreaker.data.remote.PostApiService
import com.example.circuitbreaker.data.remote.UserApiService
import com.example.circuitbreaker.data.remote.service.ktor.KtorApiClient
import com.example.circuitbreaker.domain.repo.DataRepository

class DataRepositoryImpl(
    private val ktorClient: KtorApiClient,
    private val userApiService: UserApiService,
    private val postApiService: PostApiService,
    private val useKtor: Boolean = true
) : DataRepository {

    companion object {
        private const val TAG = "DataRepository"
    }

    override suspend fun getUsers(): Result<List<UserDto>> {
        return try {
            Log.d(TAG, "Fetching users...")

            val users = if (useKtor) {
                Log.d(TAG, "Fetching users with Ktor")
                ktorClient.getUsers()
            } else {
                Log.d(TAG, "Fetching users with Retrofit")
                userApiService.getUsers()
            }

            Log.d(TAG, "Successfully fetched ${users.size} users")
            Result.success(users)

        } catch (e: CircuitBreakerOpenException) {
            Log.e(TAG, "Circuit breaker is open", e)
            Result.failure(e)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users", e)
            Result.failure(e)
        }
    }

    override suspend fun getPosts(): Result<List<PostDto>> {
        return try {
            Log.d(TAG, "Fetching posts...")

            val posts = if (useKtor) {
                Log.d(TAG, "Fetching posts with Ktor")
                ktorClient.getPosts()
            } else {
                Log.d(TAG, "Fetching posts with Retrofit")
                postApiService.getPosts()
            }

            Log.d(TAG, "Successfully fetched ${posts.size} posts")
            Result.success(posts)

        } catch (e: CircuitBreakerOpenException) {
            Log.e(TAG, "Circuit breaker is open", e)
            Result.failure(e)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching posts", e)
            Result.failure(e)
        }
    }
}