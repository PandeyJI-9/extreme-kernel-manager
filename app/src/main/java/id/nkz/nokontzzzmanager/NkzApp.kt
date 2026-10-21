package id.nkz.nokontzzzmanager

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import id.nkz.nokontzzzmanager.service.BatteryMonitorService
import id.nkz.nokontzzzmanager.utils.LocaleHelper
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import javax.inject.Inject

@HiltAndroidApp
class NkzApp : Application(), Configuration.Provider {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(base))
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()

        // Initialize Superuser shell with proper flags
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )

        // Initialize theme mode - this will be managed by ThemeManager
        // The actual theme will be applied in the MainActivity based on user preference

        // Auto-start Services if enabled when app process starts
        Log.d("NkzApp", "Application onCreate - checking for services to start")
        runCatching {
            if (preferenceManager.isBatteryMonitorEnabled()) {
                Log.d("NkzApp", "Starting BatteryMonitorService")
                BatteryMonitorService.start(this)
            }
        }
        runCatching {
            if (preferenceManager.isAppMonitorEnabled()) {
                Log.d("NkzApp", "Starting AppMonitorService")
                id.nkz.nokontzzzmanager.service.AppMonitorService.start(this)
            } else {
                Log.d("NkzApp", "AppMonitorService not enabled in preferences")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG) // Logging untuk debugging
            .build()
}