package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: PostsApiService,
) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow<Token?>(null)
    val data = _data.asStateFlow()

    val authStateFlow = data

    init {
        val id = prefs.getLong("ID_KEY", 0L)
        val token = prefs.getString("TOKEN_KEY", null)

        if (id == 0L || token == null) {
            prefs.edit { clear() }
            _data.value = null
        } else {
            _data.value = Token(id, token)
        }

        // пушится сразу после запуска приложения
        sendPushToken()
    }

    fun setAuth(id: Long, token: String) {
        prefs.edit {
            putLong("ID_KEY", id)
            putString("TOKEN_KEY", token)
        }
        _data.value = Token(id, token)
        sendPushToken()
    }

    fun removeAuth() {
        prefs.edit { clear() }
        _data.value = null
        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pushToken = token ?: Firebase.messaging.token.await()
                api.sendPushToken(PushToken(pushToken))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}