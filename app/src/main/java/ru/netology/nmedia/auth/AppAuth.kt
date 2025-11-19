package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.PushToken
import ru.netology.nmedia.dto.Token

class AppAuth private constructor(context: Context) {

    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"

        @Volatile
        private var INSTANCE: AppAuth? = null

        fun init(context: Context) {
            INSTANCE = AppAuth(context)
            INSTANCE!!.sendPushToken()
        }

        fun getInstance(): AppAuth =
            requireNotNull(INSTANCE) { "AppAuth must be initialized before usage!" }
    }

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _data = MutableStateFlow<Token?>(null)
    val data = _data.asStateFlow()
    val authStateFlow = data

    init {
        val id = prefs.getLong(ID_KEY, 0L)
        val token = prefs.getString(TOKEN_KEY, null)

        if (id == 0L || token == null) {
            prefs.edit { clear() }
            _data.value = null
        } else {
            _data.value = Token(id, token)
        }
    }

    fun setAuth(id: Long, token: String) {
        prefs.edit {
            putLong(ID_KEY, id)
            putString(TOKEN_KEY, token)
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
                PostsApi.service.sendPushToken(PushToken(pushToken))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}