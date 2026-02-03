package com.example.circuitbreaker.ui.screens


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreakerOpenException
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreakerStats
import com.example.circuitbreaker.data.dto.PostDto
import com.example.circuitbreaker.data.dto.UserDto
import com.example.circuitbreaker.data.remote.service.ktor.KtorApiClient
import com.example.circuitbreaker.domain.repo.DataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: DataRepository,
    private val ktorClient: KtorApiClient
) : ViewModel() {

    companion object {
        private const val TAG = "UserViewModel"
    }

    private val _usersState = MutableStateFlow<UiState<List<UserDto>>>(UiState.Idle)
    val usersState: StateFlow<UiState<List<UserDto>>> = _usersState.asStateFlow()

    private val _postsState = MutableStateFlow<UiState<List<PostDto>>>(UiState.Idle)
    val postsState: StateFlow<UiState<List<PostDto>>> = _postsState.asStateFlow()

    private val _ktorStatsState = MutableStateFlow<CircuitBreakerStats?>(null)
    val ktorStatsState: StateFlow<CircuitBreakerStats?> = _ktorStatsState.asStateFlow()

    fun fetchUsers() {
        Log.d(TAG, "fetchUsers() called")

        viewModelScope.launch {
            _usersState.value = UiState.Loading

            repository.getUsers()
                .onSuccess { users ->
                    _usersState.value = UiState.Success(users)
                    Log.d(TAG, "Successfully fetched ${users.size} users")
                    updateKtorStats()
                }
                .onFailure { exception ->
                    handleError(exception) { message, isCircuitOpen ->
                        _usersState.value = UiState.Error(message, isCircuitOpen)
                    }
                    updateKtorStats()
                }
        }
    }

    fun fetchPosts() {
        Log.d(TAG, "fetchPosts() called")

        viewModelScope.launch {
            _postsState.value = UiState.Loading

            repository.getPosts()
                .onSuccess { posts ->
                    _postsState.value = UiState.Success(posts)
                    Log.d(TAG, "Successfully fetched ${posts.size} posts")
                    updateKtorStats()
                }
                .onFailure { exception ->
                    handleError(exception) { message, isCircuitOpen ->
                        _postsState.value = UiState.Error(message, isCircuitOpen)
                    }
                    updateKtorStats()
                }
        }
    }

    private fun handleError(
        exception: Throwable,
        onError: (String, Boolean) -> Unit
    ) {
        when (exception) {
            is CircuitBreakerOpenException -> {
                Log.w(TAG, "Circuit breaker is open", exception)
                onError(
                    exception.message ?: "Service temporarily unavailable. Please try again later.",
                    true
                )
            }
            else -> {
                Log.e(TAG, "Error occurred", exception)
                onError(
                    exception.message ?: "An unknown error occurred",
                    false
                )
            }
        }
    }

    private fun updateKtorStats() {
        _ktorStatsState.value = ktorClient.getCircuitBreakerStats()
    }

    fun refreshStats() {
        updateKtorStats()
    }
}


sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val isCircuitBreakerOpen: Boolean = false
    ) : UiState<Nothing>()
}