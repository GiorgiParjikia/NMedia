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
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.AppActivity
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.model.Action
import ru.netology.nmedia.model.PushContent
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var appAuth: AppAuth

    private val gson = Gson()

    override fun onNewToken(token: String) {
        Log.i(TAG, "fcm_token: $token")
        appAuth.sendPushToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(TAG, "fcm_message: ${message.data}")

        val raw = message.data["content"]
        val push = raw?.let { gson.fromJson(it, PushContent::class.java) }
        val recipientId = push?.recipientId
        val content = push?.content

        val myId = appAuth.authStateFlow.value?.id ?: 0L

        // --------- PUSH logic ---------
        if (content != null) {
            when {
                recipientId == null -> {
                    showNotification("Уведомление", content)
                    return
                }

                recipientId == myId -> {
                    showNotification("Личное уведомление", content)
                    return
                }

                recipientId == 0L && myId != 0L -> {
                    appAuth.sendPushToken()
                    return
                }

                recipientId != myId -> {
                    appAuth.sendPushToken()
                    return
                }
            }
        }

        // -------- ACTIONS --------
        when (Action.from(message.data["action"])) {
            Action.LIKE -> handleLike(message.data)
            Action.NEW_POST -> handleNewPost(message.data)
            Action.UNKNOWN -> handleUnknown()
        }
    }

    // ---------------- LIKE ----------------
    private fun handleLike(data: Map<String, String>) {
        val userName = data["userName"].orEmpty()
        val postId = data["postId"].orEmpty()

        val title = if (userName.isNotBlank()) "$userName поставил лайк"
        else getString(R.string.app_name)

        val text = if (postId.isNotBlank()) "Вашему посту #$postId"
        else "Вашему посту"

        showNotification(title, text)
    }

    // ---------------- NEW POST ----------------
    private fun handleNewPost(data: Map<String, String>) {
        val userName = data["userName"].orEmpty()
        val postId = data["postId"]?.toLongOrNull() ?: 0L
        val content = data["content"].orEmpty()

        val title = if (userName.isNotBlank())
            "$userName опубликовал новый пост:"
        else getString(R.string.app_name)

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
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (canNotify()) {
            NotificationManagerCompat.from(this)
                .notify(postId.hashCode(), notif)
        }
    }

    // ---------------- UNKNOWN ----------------
    private fun handleUnknown() {
        showNotification(
            getString(R.string.app_name),
            "Пришло уведомление с неизвестным типом действия"
        )
    }

    // ---------------- COMMON NOTIFICATION ----------------
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

        if (canNotify()) {
            NotificationManagerCompat.from(this).notify(title.hashCode(), notif)
        }
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    AppActivity.CHANNEL_ID,
                    "Main notifications",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    companion object {
        private const val TAG = "FCM"
    }
}