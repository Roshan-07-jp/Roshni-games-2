package com.roshni.games.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    private var authToken: String? = null
    private var userAgent: String? = null

    fun setAuthToken(token: String?) {
        this.authToken = token
        Timber.d("Auth token updated")
    }

    fun setUserAgent(userAgent: String) {
        this.userAgent = userAgent
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()

        // Add User-Agent if available
        userAgent?.let { ua ->
            requestBuilder.header("User-Agent", ua)
        }

        // Add Authorization header if token is available
        authToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Add standard headers
        requestBuilder
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-Client-Version", "1.0.0") // TODO: Get from BuildConfig
            .header("X-Platform", "Android")

        val request = requestBuilder.build()

        Timber.d("Network Request: ${request.method} ${request.url}")

        return chain.proceed(request)
    }
}