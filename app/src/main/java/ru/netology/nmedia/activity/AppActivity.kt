package ru.netology.nmedia.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    private val viewModel by viewModels<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationsPermission()
        checkGoogleApiAvailability()
        createDefaultNotificationChannel()

        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.auth_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val authorized = viewModel.isAuthenticated

                // Показываем/скрываем группы
                menu.setGroupVisible(R.id.authorized, authorized)
                menu.setGroupVisible(R.id.unauthorized, !authorized)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val navController = findNavController(R.id.nav_host_fragment)

                return when (menuItem.itemId) {

                    R.id.signIn -> {
                        navController.navigate(R.id.signInFragment)
                        true
                    }

                    R.id.signUp -> {
                        navController.navigate(R.id.signUpFragment)
                        true
                    }

                    R.id.logout -> {
                        val currentId = navController.currentDestination?.id

                        if (currentId == R.id.newPostFragment) {
                            AlertDialog.Builder(this@AppActivity)
                                .setTitle(R.string.logout)
                                .setMessage(R.string.logout_confirm_while_editing)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    AppAuth.getInstance().removeAuth()
                                    navController.popBackStack()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()

                        } else {
                            AlertDialog.Builder(this@AppActivity)
                                .setTitle(R.string.logout)
                                .setMessage(R.string.logout_confirm)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    AppAuth.getInstance().removeAuth()
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        addMenuProvider(menuProvider, this)

        // обновление меню при авторизации
        viewModel.data.observe(this) {
            invalidateMenu()
        }

        // FCM токен
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.i(TAG, token)
        }

        // переход по уведомлению
        handleIntentIfAny(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentIfAny(intent)
    }

    private fun handleIntentIfAny(intent: Intent?) {
        val postId = intent?.getLongExtra("postId", 0L) ?: 0L
        if (postId != 0L) {
            findNavController(R.id.nav_host_fragment).navigate(
                R.id.action_feedFragment_to_singlePostFragment,
                bundleOf("postId" to postId)
            )
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