package com.roshni.games.dynamicfeature

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Manages dynamic feature modules for on-demand game downloads
 */
class DynamicFeatureManager private constructor(
    private val context: Context
) {

    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)
    private val _installationState = MutableStateFlow<InstallationState>(InstallationState.Idle)
    val installationState: StateFlow<InstallationState> = _installationState

    private val listener = SplitInstallStateUpdatedListener { state ->
        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                _installationState.value = InstallationState.Downloading(
                    progress = state.bytesDownloaded() * 100 / state.totalBytesToDownload(),
                    module = state.moduleNames().firstOrNull() ?: ""
                )
            }
            SplitInstallSessionStatus.INSTALLING -> {
                _installationState.value = InstallationState.Installing(
                    module = state.moduleNames().firstOrNull() ?: ""
                )
            }
            SplitInstallSessionStatus.INSTALLED -> {
                _installationState.value = InstallationState.Installed(
                    module = state.moduleNames().firstOrNull() ?: ""
                )
            }
            SplitInstallSessionStatus.FAILED -> {
                _installationState.value = InstallationState.Failed(
                    module = state.moduleNames().firstOrNull() ?: "",
                    error = state.errorCode()
                )
            }
            SplitInstallSessionStatus.CANCELING -> {
                _installationState.value = InstallationState.Canceling(
                    module = state.moduleNames().firstOrNull() ?: ""
                )
            }
            SplitInstallSessionStatus.CANCELED -> {
                _installationState.value = InstallationState.Canceled(
                    module = state.moduleNames().firstOrNull() ?: ""
                )
            }
            else -> {
                Timber.d("Dynamic feature state: ${state.status()}")
            }
        }
    }

    init {
        splitInstallManager.registerListener(listener)
    }

    /**
     * Downloads and installs a game module
     */
    suspend fun installGameModule(gameModule: String): Result<Unit> {
        return try {
            val request = SplitInstallRequest.newBuilder()
                .addModule(gameModule)
                .build()

            splitInstallManager.startInstall(request)

            // Wait for installation to complete
            var attempts = 0
            while (attempts < 60) { // 60 second timeout
                when (val state = _installationState.value) {
                    is InstallationState.Installed -> return Result.success(Unit)
                    is InstallationState.Failed -> return Result.failure(
                        Exception("Installation failed with error code: ${state.error}")
                    )
                    is InstallationState.Canceled -> return Result.failure(
                        Exception("Installation was canceled")
                    )
                    else -> {
                        kotlinx.coroutines.delay(1000)
                        attempts++
                    }
                }
            }

            Result.failure(Exception("Installation timeout"))
        } catch (e: Exception) {
            Timber.e(e, "Failed to install game module: $gameModule")
            Result.failure(e)
        }
    }

    /**
     * Checks if a game module is installed
     */
    fun isGameModuleInstalled(gameModule: String): Boolean {
        return splitInstallManager.installedModules.contains(gameModule)
    }

    /**
     * Gets the list of installed game modules
     */
    fun getInstalledGameModules(): Set<String> {
        return splitInstallManager.installedModules.filter { module ->
            module.startsWith("game:")
        }.toSet()
    }

    /**
     * Uninstalls a game module
     */
    fun uninstallGameModule(gameModule: String) {
        splitInstallManager.deferredUninstall(listOf(gameModule))
    }

    /**
     * Cleans up resources
     */
    fun cleanup() {
        splitInstallManager.unregisterListener(listener)
    }

    sealed class InstallationState {
        object Idle : InstallationState()
        data class Downloading(val progress: Long, val module: String) : InstallationState()
        data class Installing(val module: String) : InstallationState()
        data class Installed(val module: String) : InstallationState()
        data class Failed(val module: String, val error: Int) : InstallationState()
        data class Canceling(val module: String) : InstallationState()
        data class Canceled(val module: String) : InstallationState()
    }

    companion object {
        @Volatile
        private var INSTANCE: DynamicFeatureManager? = null

        fun getInstance(context: Context): DynamicFeatureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicFeatureManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}