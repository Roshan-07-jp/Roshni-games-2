package com.roshni.games.service.analytics.data.model

import kotlinx.serialization.Serializable

/**
 * Data models for analytics and crash reporting
 */

/**
 * Analytics event
 */
@Serializable
data class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val userId: String? = null
)

/**
 * Crash report data
 */
@Serializable
data class CrashReport(
    val id: String,
    val message: String,
    val stackTrace: String,
    val cause: String? = null,
    val deviceInfo: DeviceInfo,
    val appInfo: AppInfo,
    val userId: String? = null,
    val sessionId: String,
    val timestamp: Long,
    val breadcrumbs: List<String> = emptyList(),
    val customData: Map<String, Any> = emptyMap()
)

/**
 * Device information
 */
@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val screenResolution: String,
    val density: String,
    val locale: String,
    val timezone: String,
    val totalMemory: Long,
    val availableMemory: Long
)

/**
 * Application information
 */
@Serializable
data class AppInfo(
    val version: String,
    val buildNumber: String,
    val packageName: String,
    val installTime: Long,
    val lastUpdateTime: Long
)

/**
 * Analytics configuration
 */
@Serializable
data class AnalyticsConfig(
    val enabled: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val performanceMonitoringEnabled: Boolean = false,
    val userAnalyticsEnabled: Boolean = false,
    val sessionTimeoutMinutes: Long = 30,
    val maxEventsPerSession: Int = 100,
    val flushIntervalSeconds: Long = 60,
    val enableBreadcrumbs: Boolean = true,
    val maxBreadcrumbs: Int = 50
)

/**
 * Analytics session
 */
@Serializable
data class AnalyticsSession(
    val id: String,
    val userId: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val eventCount: Int = 0,
    val duration: Long? = null,
    val isActive: Boolean = true
)

/**
 * Performance metrics
 */
@Serializable
data class PerformanceMetrics(
    val sessionId: String,
    val gameLoadTime: Long? = null,
    val frameRate: Float? = null,
    val memoryUsage: Long? = null,
    val batteryLevel: Int? = null,
    val networkLatency: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User consent for analytics
 */
@Serializable
data class UserConsent(
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = false,
    val performanceMonitoringEnabled: Boolean = false,
    val dataCollectionEnabled: Boolean = false,
    val consentTimestamp: Long = System.currentTimeMillis(),
    val consentVersion: String = "1.0"
)

/**
 * Analytics statistics
 */
@Serializable
data class AnalyticsStats(
    val totalEvents: Long = 0,
    val totalSessions: Long = 0,
    val crashesReported: Long = 0,
    val averageSessionDuration: Long = 0,
    val lastActivity: Long? = null,
    val dataSize: Long = 0 // in bytes
)