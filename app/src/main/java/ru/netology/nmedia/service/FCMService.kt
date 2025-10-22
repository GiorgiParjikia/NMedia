package ru.netology.nmedia.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity
import ru.netology.nmedia.model.Action

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(TAG, "fcm_token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(TAG, "fcm_message: ${message.data}")

        val action = Action.from(message.data["action"])

        when (action) {
            Action.LIKE      -> handleLike(message.data)
            Action.NEW_POST  -> handleNewPost(message.data)
            Action.UNKNOWN   -> handleUnknown(message.data)
        }
    }

    private fun handleLike(data: Map<String, String>) {
        val userName = data["userName"].orEmpty()
        val postId   = data["postId"].orEmpty()

        val title = if (userName.isNotBlank()) {
            "$userName поставил лайк"
        } else {
            getString(R.string.app_name)
        }

        val text = if (postId.isNotBlank()) "Вашему посту #$postId" else "Вашему посту"
        showNotification(title, text)
    }

    private fun handleNewPost(data: Map<String, String>) {
        val userName = data["userName"].orEmpty()
        val postId   = data["postId"]?.toLongOrNull() ?: 0L
        val content  = data["content"].orEmpty().ifBlank { "Новый пост" }

        val title = if (userName.isNotBlank())
            "$userName опубликовал новый пост:"
        else
            getString(R.string.app_name)

        val intent = Intent(this, AppActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("postId", postId)
        }
        val pi = PendingIntent.getActivity(
            this,
            postId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        ensureChannel()

        val notif = NotificationCompat.Builder(this, AppActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content.take(80))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(content)
                    .setSummaryText(getString(R.string.app_name))
            )
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(postId.hashCode(), notif)
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS not granted, skip notify()")
        }
    }

    private fun handleUnknown(data: Map<String, String>) {
        Log.w(TAG, "Unknown action: ${data["action"]}; payload=$data")
        showNotification(
            title = getString(R.string.app_name),
            text = "Пришло уведомление с неизвестным типом действия"
        )
    }

    private fun showNotification(title: String, text: String) {
        val intent = Intent(this, AppActivity::class.java)
        val pi = PendingIntent.getActivity(
            this,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        ensureChannel()

        val notif = NotificationCompat.Builder(this, AppActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(title.hashCode(), notif)
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS not granted, skip notify()")
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                AppActivity.CHANNEL_ID,
                "Main notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}