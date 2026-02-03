package com.example.circuitbreaker

import android.app.Application
import android.os.StrictMode
import com.example.circuitbreaker.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level



class CircuitBreakerApp : Application() {


    override fun onCreate() {
        super.onCreate()
        startKoin()
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }



    private fun startKoin() {
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@CircuitBreakerApp)
            modules(
                appModule
            )
        }
    }
}
