package com.roshni.games.core.network.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.roshni.games.core.network.interceptor.AdBlockInterceptor
import com.roshni.games.core.network.interceptor.AuthInterceptor
import com.roshni.games.core.network.interceptor.LoggingInterceptor
import com.roshni.games.core.network.service.GameApiService
import com.roshni.games.core.network.service.PlayerApiService
import com.roshni.games.core.network.service.ScoreApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        adBlockInterceptor: AdBlockInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(adBlockInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.roshnigames.com/") // TODO: Move to BuildConfig
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGameApiService(retrofit: Retrofit): GameApiService {
        return retrofit.create(GameApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePlayerApiService(retrofit: Retrofit): PlayerApiService {
        return retrofit.create(PlayerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideScoreApiService(retrofit: Retrofit): ScoreApiService {
        return retrofit.create(ScoreApiService::class.java)
    }
}