package com.roshni.games.core.network.model

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errorCode: String?,
    val timestamp: String
)