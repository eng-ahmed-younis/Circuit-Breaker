package com.example.circuitbreaker.data.circuitbreaker

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response


/**
 * OkHttp Interceptor that adds Circuit Breaker protection to Retrofit
 *
 * @param circuitBreaker The circuit breaker instance to use
 */
class CircuitBreakerInterceptor(
    private val circuitBreaker: CircuitBreaker
) : Interceptor {

    /**
     * Intercepts each HTTP request and wraps it with circuit breaker logic
     *
     * @param chain The interceptor chain
     * @return Response from the server
     * @throws CircuitBreakerOpenException if circuit is open
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // Use runBlocking because Interceptor.intercept is not a suspend function
        // but our circuit breaker requires suspend
        return runBlocking {
            circuitBreaker.execute {
                // Proceed with the actual HTTP request
                chain.proceed(chain.request())
            }
        }
    }


}