package com.roshni.games.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggingInterceptor @Inject constructor() : Interceptor {

    private val httpLoggingInterceptor = HttpLoggingInterceptor { message ->
        Timber.tag("OkHttp").d(message)
    }.apply {
        level = if (com.roshni.games.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.nanoTime()

        Timber.d("Sending request: ${request.method} ${request.url}")

        return try {
            val response = chain.proceed(request)
            val endTime = System.nanoTime()
            val duration = (endTime - startTime) / 1_000_000 // Convert to milliseconds

            Timber.d("Received response: ${response.code} in ${duration}ms")

            if (!response.isSuccessful) {
                Timber.w("Request failed: ${request.method} ${request.url} - ${response.code}")
            }

            response
        } catch (e: Exception) {
            val endTime = System.nanoTime()
            val duration = (endTime - startTime) / 1_000_000

            Timber.e(e, "Request failed after ${duration}ms: ${request.method} ${request.url}")
            throw e
        }
    }
}