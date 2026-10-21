package id.nkz.nokontzzzmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.AndroidEntryPoint
import id.nkz.nokontzzzmanager.R
import id.nkz.nokontzzzmanager.data.repository.DexoptRepository
import id.nkz.nokontzzzmanager.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DexoptService : Service() {

    @Inject
    lateinit var dexoptRepository: DexoptRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "dexopt_channel"
    private val NOTIFICATION_ID = 2001
    private val FINISHED_NOTIFICATION_ID = 2002

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DexoptService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
             context.stopService(Intent(context, DexoptService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        runDexopt()
        return START_NOT_STICKY
    }

    private var dedicatedShell: Shell? = null

    private fun runDexopt() {
        dexoptRepository.setRunning(true)
        
        serviceScope.launch {
            // Buffer to hold the latest log
            var lastLogMessage = "Initializing..."
            
            try {
                // Build a new root shell instance
                dedicatedShell = Shell.Builder.create()
                    .build()
            } catch (e: Exception) {
                dexoptRepository.updateLastLog("Error creating shell: ${e.message}")
                stopSelf()
                return@launch
            }

            val callbackList = object : CallbackList<String>() {
                override fun onAddElement(s: String) {
                    lastLogMessage = s
                }
            }

            // Start a coroutine to push updates periodically
            val updaterJob = launch {
                while (isActive) {
                    dexoptRepository.updateLastLog(lastLogMessage)
                    delay(200) // Update UI 5 times a second
                }
            }

            try {
                dexoptRepository.updateLastLog("Starting optimization (Dedicated Shell)...")
                
                val commands = mutableListOf(
                    "nice -n 19 cmd package compile -a -f -m speed-profile"
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    commands.add("nice -n 19 cmd package compile -a -f --compile-layouts")
                }

                // Execute commands on the DEDICATED shell instance
                dedicatedShell?.newJob()?.add(*commands.toTypedArray())?.to(callbackList)?.exec()

                if (isActive) {
                    dexoptRepository.updateLastLog("Dexopt process finished.")
                    dexoptRepository.setFinished(true)
                    showFinishedNotification()
                }
            } catch (e: Exception) {
                if (isActive) {
                    dexoptRepository.updateLastLog("Error: ${e.message}")
                }
            } finally {
                updaterJob.cancel()
                
                try {
                    dedicatedShell?.close()
                    dedicatedShell = null
                } catch (e: Exception) { /* ignore */ }
                
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Dexopt Service"
        val descriptionText = "Shows notification when Dexopt is running"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dexopt Running")
            .setContentText("Optimizing apps in background...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFinishedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dexopt Finished")
            .setContentText("Optimization process completed successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // If service is destroyed (Stop button pressed), kill the compile processes
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Terminate the shell if it's still running
                dedicatedShell?.close()
                
                // Force kill the android compile processes to stop them immediately
                Shell.cmd("pkill -f \"package compile\"").exec()
                Shell.cmd("pkill -f \"pm compile\"").exec()
                
                dexoptRepository.updateLastLog("Process stopped by user.")
                dexoptRepository.setCanceled(true)
            } catch (e: Exception) {
                // ignore
            } finally {
                serviceScope.cancel()
            }
        }
    }
}
