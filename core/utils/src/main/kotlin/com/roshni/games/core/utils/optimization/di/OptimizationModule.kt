package com.roshni.games.core.utils.optimization.di

import android.content.Context
import com.roshni.games.core.utils.optimization.AdaptiveOptimizationSystem
import com.roshni.games.core.utils.optimization.OptimizationIntegration
import com.roshni.games.core.utils.optimization.PerformanceOptimizationFramework
import com.roshni.games.core.utils.optimization.PerformanceOptimizationFrameworkImpl
import com.roshni.games.core.utils.optimization.ResourceConstraintManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OptimizationModule {

    @Provides
    @Singleton
    fun providePerformanceOptimizationFramework(
        @ApplicationContext context: Context,
        performanceMonitor: com.roshni.games.core.utils.performance.PerformanceMonitor,
        batteryOptimizer: com.roshni.games.core.utils.optimization.BatteryOptimizer
    ): PerformanceOptimizationFramework {
        return PerformanceOptimizationFrameworkImpl(
            performanceMonitor = performanceMonitor,
            batteryOptimizer = batteryOptimizer,
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideAdaptiveOptimizationSystem(
        performanceMonitor: com.roshni.games.core.utils.performance.PerformanceMonitor,
        batteryOptimizer: com.roshni.games.core.utils.optimization.BatteryOptimizer,
        optimizationFramework: PerformanceOptimizationFramework
    ): AdaptiveOptimizationSystem {
        return AdaptiveOptimizationSystem(
            performanceMonitor = performanceMonitor,
            batteryOptimizer = batteryOptimizer,
            optimizationFramework = optimizationFramework
        )
    }

    @Provides
    @Singleton
    fun provideResourceConstraintManager(
        optimizationFramework: PerformanceOptimizationFramework,
        adaptiveSystem: AdaptiveOptimizationSystem
    ): ResourceConstraintManager {
        return ResourceConstraintManager(
            optimizationFramework = optimizationFramework,
            adaptiveSystem = adaptiveSystem
        )
    }

    @Provides
    @Singleton
    fun provideOptimizationIntegration(
        @ApplicationContext context: Context,
        performanceMonitor: com.roshni.games.core.utils.performance.PerformanceMonitor,
        batteryOptimizer: com.roshni.games.core.utils.optimization.BatteryOptimizer
    ): OptimizationIntegration {
        return OptimizationIntegration(
            context = context,
            performanceMonitor = performanceMonitor,
            batteryOptimizer = batteryOptimizer
        )
    }
}