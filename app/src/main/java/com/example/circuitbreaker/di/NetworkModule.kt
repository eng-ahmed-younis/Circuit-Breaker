package com.example.circuitbreaker.di

import com.example.circuitbreaker.data.circuitbreaker.CircuitBreaker
import com.example.circuitbreaker.data.circuitbreaker.CircuitBreakerInterceptor
import com.example.circuitbreaker.data.remote.PostApiService
import com.example.circuitbreaker.data.remote.UserApiService
import com.example.circuitbreaker.data.remote.service.ktor.KtorApiClient
import com.example.circuitbreaker.domain.repo.DataRepository
import com.example.circuitbreaker.repository.DataRepositoryImpl
import com.example.circuitbreaker.ui.screens.UserViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.qualifier.named
import org.koin.dsl.module


import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Koin dependency injection module
 * Defines how to create and provide all dependencies
 */
val appModule = module {


    single(named("ktor")) {
        CircuitBreaker(
            name = "KtorCircuitBreaker",
            failureThreshold = 3,      // Open after 3 failures
            resetTimeout = 30000L,     // Test recovery after 30 seconds
            halfOpenMaxAttempts = 2    // Allow 2 test attempts
        )
    }

    /**
     * Provides Circuit Breaker for Retrofit
     * Named qualifier to distinguish from Ktor's circuit breaker
     */
    single(named("retrofit")) {
        CircuitBreaker(
            name = "RetrofitCircuitBreaker",
            failureThreshold = 3,
            resetTimeout = 30000L,
            halfOpenMaxAttempts = 2
        )
    }


    /**
     * Provides configured Ktor HttpClient
     * Singleton instance shared across the app
     */
    single {
        HttpClient(Android) {
            // Install JSON content negotiation

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    explicitNulls = true
                    encodeDefaults = true
                })
            }

            // Install logging
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }

            // Install timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            // Default request configuration
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }


    /**
     * Provides KtorApiClient
     * Depends on HttpClient and Circuit Breaker
     */
    single {
        KtorApiClient(
            client = get(),
            circuitBreaker = get(named("ktor"))
        )
    }


    /**
     * Provides Circuit Breaker Interceptor for Retrofit
     */
    single {
        CircuitBreakerInterceptor(
            circuitBreaker = get(named("retrofit"))
        )
    }


    /**
     * Provides HttpLoggingInterceptor for Retrofit
     */
    single<HttpLoggingInterceptor> {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }


    /**
     * Provides configured OkHttpClient
     */
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .addInterceptor(get<CircuitBreakerInterceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }


    /**
     * Provides configured Retrofit instance
     */
    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("http://10.100.20.32:3001/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    /**
     * Provides Retrofit ApiService
     */
    single<PostApiService> {
        get<Retrofit>().create(PostApiService::class.java)
    }

    single <UserApiService>{
        get <Retrofit>().create(UserApiService::class.java)
    }


    single<DataRepository> {
        DataRepositoryImpl(
            ktorClient = get(),
            userApiService = get(),
            postApiService = get(),
            useKtor = true
        )
    }

    viewModel {
        UserViewModel(
            repository = get(),
            ktorClient = get()
        )
    }
}

