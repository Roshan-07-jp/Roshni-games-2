package com.roshni.games

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RoshniGamesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize other third-party libraries here
        initializeThirdPartyLibraries()
    }

    private fun initializeThirdPartyLibraries() {
        // Initialize Firebase
        // FirebaseApp.initializeApp(this)

        // Initialize other services as needed
        // SentryAndroid.init(this) { options ->
        //     options.dsn = BuildConfig.SENTRY_DSN
        // }

        // Initialize analytics
        // MixpanelAPI.getInstance(this, BuildConfig.MIXPANEL_TOKEN)
    }
}