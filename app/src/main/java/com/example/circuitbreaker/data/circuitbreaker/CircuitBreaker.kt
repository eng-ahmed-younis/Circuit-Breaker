package com.example.circuitbreaker.data.circuitbreaker

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Circuit Breaker implementation.
 *
 * Temporarily stops requests to a failing service to avoid repeated failures,
 * then retries after a short delay to check if the service has recovered.
 *
 * @param name Name of the circuit breaker (used for logging)
 * @param failureThreshold Failures needed to open the circuit
 * @param resetTimeout Time (ms) to wait before retrying requests
 * @param halfOpenMaxAttempts Allowed test requests during recovery
 */


class CircuitBreaker(
    private val name: String = "CircuitBreaker",
    private val failureThreshold: Int = 5,
    private val resetTimeout: Long = 60000L, // 1 minute in milliseconds,
    private val halfOpenMaxAttempts: Int = 3
) {

    companion object {
        private const val TAG = "CircuitBreaker"
    }

    // Number of repeated failures
    private var failureCount = 0

    // Time of the last failure
    private var lastFailureTime = 0L


    // Number of attempts in HALF_OPEN state
    private var halfOpenAttempts = 0

    // Mutex for thread-safe state management
    private val mutex = Mutex()


    // Current state of the circuit breaker
    private var state = State.CLOSED


    /**
     * Circuit Breaker States:
     * CLOSED - Normal operation, all requests pass through
     * OPEN - Too many failures detected, requests are blocked
     * HALF_OPEN - Testing if the service has recovered
     */
    enum class State {
        CLOSED,      // Everything is working normally
        OPEN,        // Service is failing, blocking all requests ----> waite for  resetTimeout pass
        HALF_OPEN    // Testing recovery with limited requests
        /** [HALF_OPEN] (Test)
         * We allow a small number of requests.
         * If successful → we revert to CLOSED.
         * If unsuccessful → we reopen to OPEN.
         * */
    }


    suspend fun <T> execute(block: suspend () -> T): T {
        // 1️⃣ Check & update state under lock
        mutex.withLock {
            Log.d(TAG, "[$name] Current state: $state, Failures: $failureCount")

            when (state) {
                State.OPEN -> {
                    val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime
                    if (timeSinceLastFailure >= resetTimeout) {
                        state = State.HALF_OPEN
                        halfOpenAttempts = 0
                    } else {
                        val remainingTime = (resetTimeout - timeSinceLastFailure) / 1000
                        throw CircuitBreakerOpenException(
                            "Circuit breaker is OPEN. Retry in $remainingTime seconds."
                        )
                    }
                }

                State.HALF_OPEN -> {
                    if (halfOpenAttempts >= halfOpenMaxAttempts) {
                        throw CircuitBreakerOpenException(
                            "Circuit breaker is testing recovery. Please wait."
                        )
                    }
                    halfOpenAttempts++
                }

                State.CLOSED -> Unit
            }
        }

        // 2️⃣ Execute network call OUTSIDE mutex
        return try {
            val result = block()

            // 3️⃣ Update success state under lock
            mutex.withLock {
                onSuccess()
            }

            result
        } catch (e: Exception) {
            // 4️⃣ Update failure state under lock
            mutex.withLock {
                onFailure(e)
            }
            throw e
        }
    }



    /**
     * Handles successful requests
     * Updates the circuit state
     */
    private fun onSuccess() {

        when(state){

            State.HALF_OPEN -> {
                // Success in HALF_OPEN means service recovered
                Log.i(TAG, "[$name] Service recovered! Closing circuit")

                // Close the circuit and reset counters
                state = State.CLOSED
                failureCount = 0
                halfOpenAttempts = 0
            }

            State.CLOSED -> {
                // Success in CLOSED state, reset failure counter
                if (failureCount > 0) {
                    Log.d(TAG, "[$name] Request succeeded, resetting failure count")
                }
                failureCount = 0
            }

            State.OPEN -> {
                // Should not happen - requests shouldn't reach here when OPEN
                Log.w(TAG, "[$name] Unexpected success in OPEN state")
            }
        }
    }



    /**
     * Handles failed request execution
     * Updates failure counters and state transitions
     */
    private fun onFailure(exception: Exception) {
        // Increment failure counter
        failureCount++

        // Record the time of this failure
        lastFailureTime = System.currentTimeMillis()

        Log.e(TAG, "[$name] Request failed: ${exception.message}")

        when (state) {
            State.HALF_OPEN -> {
                // Failure during recovery test means service still not ready
                Log.w(TAG, "[$name] Recovery test failed, reopening circuit")
                halfOpenAttempts++

                // Reopen the circuit
                state = State.OPEN
            }
            State.CLOSED -> {
                // Check if we've hit the failure threshold
                if (failureCount >= failureThreshold) {
                    // Too many failures, open the circuit
                    Log.w(TAG, "[$name] Failure threshold reached ($failureCount/$failureThreshold), opening circuit")
                    state = State.OPEN
                } else {
                    Log.d(TAG, "[$name] Failure count: $failureCount/$failureThreshold")
                }
            }
            State.OPEN -> {
                // Already open, just update the failure time
                Log.d(TAG, "[$name] Additional failure while circuit is OPEN")
            }
        }
    }


    /**
     * Returns the current state of the circuit breaker
     * Useful for monitoring and logging
     */
    fun getState() = state

    /**
     * Returns the current failure count
     */
    fun getFailureCount() = failureCount



    /**
     * Returns circuit breaker statistics for monitoring
     */
    fun getStats(): CircuitBreakerStats {
        return CircuitBreakerStats(
            name = name,
            state = state,
            failureCount = failureCount,
            failureThreshold = failureThreshold,
            lastFailureTime = lastFailureTime,
            halfOpenAttempts = halfOpenAttempts
        )
    }



    /**
     * Manually reset the circuit breaker (use with caution)
     */
    suspend fun reset() = mutex.withLock {
        Log.i(TAG, "[$name] Manual reset triggered")
        state = State.CLOSED
        failureCount = 0
        halfOpenAttempts = 0
        lastFailureTime = 0L
    }


}


/**
 * Data class containing circuit breaker statistics
 */
data class CircuitBreakerStats(
    val name: String,
    val state: CircuitBreaker.State,
    val failureCount: Int,
    val failureThreshold: Int,
    val lastFailureTime: Long,
    val halfOpenAttempts: Int
)




class CircuitBreakerOpenException(message: String) : Exception(message)
