package mqtt.client

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class PingWorker(context: Context, val params: WorkerParameters) : CoroutineWorker(context.applicationContext, params) {


    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val waitingInfo = createForegroundInfo(applicationContext, id, "waiting", "waiting")
        if(shouldForeground) setForeground(waitingInfo)
        println("get service connection")
        val serviceConnection = MqttAppServiceConnection.getMqttServiceConnection(applicationContext, this)
        val pingInfo = createForegroundInfo(applicationContext, id, "pinging", "pinging")
        if(shouldForeground) setForeground(pingInfo)
        val sucesses = serviceConnection.pingAsync().await()
        val doneInfo = createForegroundInfo(applicationContext, id, "pinging", "done")
        if(shouldForeground) setForeground(doneInfo)
        val total = serviceConnection.findConnections().associateBy { it.connectionId }
        val failures = total.keys - sucesses
        if (failures.isEmpty()) {
            Result.success()
        } else {
            val failedInfo = createForegroundInfo(applicationContext, id, "failed ${failures.count()}", "pinged")
            if (shouldForeground) setForeground(failedInfo)
            Result.retry()
        }
    }

    companion object {
        var shouldForeground = false
        val notificationId = Int.MIN_VALUE/2
        val channelId = "mqtt"


        fun createForegroundInfo(context: Context, id: UUID?, progress: String, title:String): ForegroundInfo {
            val applicationContext = context.applicationContext

            // Create a Notification channel if necessary
            maybeCreateChannel(applicationContext)

            val pingIntent = Intent(context, MqttService::class.java)
            pingIntent.putExtra("ping", true)
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle(title)
                .setTicker(title)
                .setContentText(progress)
                .setSmallIcon(android.R.drawable.btn_default_small)
//            // Add the cancel action to the notification which can
//            // be used to cancel the worker
                .addAction(android.R.drawable.ic_input_add, "ping", PendingIntent.getService(applicationContext, 1337, pingIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(android.R.drawable.ic_delete, "cancel", if (id != null)WorkManager.getInstance(applicationContext)
                    .createCancelPendingIntent(id) else null)
                .build()


            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!shouldForeground) notificationManager.notify(notificationId, notification)
            return ForegroundInfo(notificationId, notification)
        }

        private fun maybeCreateChannel(applicationContext: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel
                val channel = NotificationChannel(channelId, "Mqtt Status", IMPORTANCE_DEFAULT)
                channel.description = "mqtt connectivity status"
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}