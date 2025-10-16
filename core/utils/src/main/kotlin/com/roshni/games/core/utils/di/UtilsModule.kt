package com.roshni.games.core.utils.di

import android.app.Activity
import android.content.Context
import com.roshni.games.core.utils.error.ErrorHandlingFramework
import com.roshni.games.core.utils.error.ErrorHandlingFrameworkImpl
import com.roshni.games.core.utils.error.ErrorPresentation
import com.roshni.games.core.utils.error.ErrorPresentationManager
import com.roshni.games.core.utils.error.ErrorRecoveryStrategyFactory
import com.roshni.games.core.utils.error.ErrorReporting
import com.roshni.games.core.utils.error.ErrorReportingManager
import com.roshni.games.core.utils.integration.SystemIntegrationHub
import com.roshni.games.core.utils.notification.NotificationDeliveryManager
import com.roshni.games.core.utils.notification.NotificationDeliveryManagerImpl
import com.roshni.games.core.utils.notification.NotificationIntegrationManager
import com.roshni.games.core.utils.notification.NotificationIntegrationManagerImpl
import com.roshni.games.core.utils.notification.NotificationManager
import com.roshni.games.core.utils.notification.NotificationManagerImpl
import com.roshni.games.core.utils.notification.NotificationPreferenceManager
import com.roshni.games.core.utils.notification.NotificationPreferenceManagerImpl
import com.roshni.games.core.utils.notification.NotificationScheduler
import com.roshni.games.core.utils.notification.NotificationSchedulerImpl
import com.roshni.games.core.utils.permission.AndroidPermissionHandler
import com.roshni.games.core.utils.permission.FeaturePermissionManager
import com.roshni.games.core.utils.permission.PermissionEducationManager
import com.roshni.games.core.utils.security.SecurityManager
import com.roshni.games.core.utils.security.SecurityManagerImpl
import com.roshni.games.core.utils.security.events.SecurityEventHandler
import com.roshni.games.core.utils.security.integration.SecurityServiceIntegration
import com.roshni.games.core.utils.security.permissions.AdvancedPermissionManager
import com.roshni.games.core.utils.security.validation.SessionValidator
import com.roshni.games.feature.parentalcontrols.domain.SecurityService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideSecurityManager(securityService: SecurityService): SecurityManager {
        return SecurityManagerImpl(securityService)
    }

    @Provides
    @Singleton
    fun provideAdvancedPermissionManager(): AdvancedPermissionManager {
        return AdvancedPermissionManager()
    }

    @Provides
    @Singleton
    fun provideSessionValidator(): SessionValidator {
        return SessionValidator()
    }

    @Provides
    @Singleton
    fun provideSecurityEventHandler(
        @ApplicationScope scope: CoroutineScope
    ): SecurityEventHandler {
        return SecurityEventHandler(scope)
    }

    @Provides
    @Singleton
    fun provideSecurityServiceIntegration(securityService: SecurityService): SecurityServiceIntegration {
        return SecurityServiceIntegration(securityService)
    }

    @Provides
    @Singleton
    fun provideNotificationPreferenceManager(): NotificationPreferenceManager {
        return NotificationPreferenceManagerImpl()
    }

    @Provides
    @Singleton
    fun provideNotificationDeliveryManager(
        notificationPreferenceManager: NotificationPreferenceManager
    ): NotificationDeliveryManager {
        return NotificationDeliveryManagerImpl(notificationPreferenceManager)
    }

    @Provides
    @Singleton
    fun provideNotificationScheduler(
        notificationManager: NotificationManager
    ): NotificationScheduler {
        return NotificationSchedulerImpl(notificationManager)
    }

    @Provides
    @Singleton
    fun provideNotificationIntegrationManager(
        notificationManager: NotificationManager,
        systemIntegrationHub: SystemIntegrationHub
    ): NotificationIntegrationManager {
        return NotificationIntegrationManagerImpl(notificationManager, systemIntegrationHub)
    }

    @Provides
    @Singleton
    fun provideNotificationManager(
        notificationPreferenceManager: NotificationPreferenceManager,
        notificationDeliveryManager: NotificationDeliveryManager,
        notificationScheduler: NotificationScheduler,
        systemIntegrationHub: SystemIntegrationHub,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationScope applicationScope: CoroutineScope
    ): NotificationManager {
        return NotificationManagerImpl(
            notificationPreferenceManager,
            notificationDeliveryManager,
            notificationScheduler,
            systemIntegrationHub,
            ioDispatcher,
            applicationScope
        )
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideFeaturePermissionManager(): FeaturePermissionManager {
        return FeaturePermissionManager()
    }

    @Provides
    @Singleton
    fun providePermissionEducationManager(): PermissionEducationManager {
        return PermissionEducationManager()
    }

    @Provides
    @Singleton
    fun provideAndroidPermissionHandler(
        context: Context,
        featurePermissionManager: FeaturePermissionManager,
        permissionEducationManager: PermissionEducationManager,
        @ApplicationScope scope: CoroutineScope
    ): AndroidPermissionHandler {
        // In a real implementation, you would need to provide an activity provider
        // For now, we'll use a simple provider that returns null
        val activityProvider = { null as Activity? }
        return AndroidPermissionHandler(context, activityProvider)
    }

    // Error Handling Framework
    @Provides
    @Singleton
    fun provideErrorHandlingFramework(
        systemIntegrationHub: SystemIntegrationHub,
        @ApplicationScope scope: CoroutineScope
    ): ErrorHandlingFramework {
        return ErrorHandlingFrameworkImpl(systemIntegrationHub, scope)
    }

    @Provides
    @Singleton
    fun provideErrorReporting(
        @ApplicationScope scope: CoroutineScope
    ): ErrorReporting {
        return ErrorReporting(scope)
    }

    @Provides
    @Singleton
    fun provideErrorReportingManager(
        errorReporting: ErrorReporting
    ): ErrorReportingManager {
        return ErrorReportingManager(errorReporting)
    }

    @Provides
    @Singleton
    fun provideErrorPresentationManager(): ErrorPresentationManager {
        return ErrorPresentationManager()
    }

    @Provides
    @Singleton
    fun provideErrorRecoveryStrategyFactory(): ErrorRecoveryStrategyFactory {
        return ErrorRecoveryStrategyFactory
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope