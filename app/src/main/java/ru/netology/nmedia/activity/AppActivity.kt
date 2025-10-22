package ru.netology.nmedia.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationsPermission()
        checkGoogleApiAvailability()
        createDefaultNotificationChannel()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.i(TAG, "fcm_token: $token")
        }

        // обработка запуска из уведомления
        handleIntentIfAny(intent)
    }

    // ВАЖНО: сигнатура без nullable и без 'protected'
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)               // setIntent требует не-null
        handleIntentIfAny(intent)
    }

    // Обработчик извлекает postId, если он есть
    private fun handleIntentIfAny(intent: Intent?) {
        val postId = intent?.getLongExtra("postId", 0L) ?: 0L
        if (postId != 0L) {
            Log.i(TAG, "Open from notification, postId=$postId")

            val navController = findNavController(R.id.nav_host_fragment)
            val args = bundleOf("postId" to postId)
            navController.navigate(R.id.action_feedFragment_to_singlePostFragment, args)
        }
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code != ConnectionResult.SUCCESS && isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
            }
        }
    }

    private fun createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "FCM"
        const val CHANNEL_ID = "default"
    }
}