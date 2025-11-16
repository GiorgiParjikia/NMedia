package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nmedia.dto.Token

class AppAuth private constructor(context: Context) {
    companion object {
        private const val ID_KEY = "ID_KEY"
        private const val TOKEN_KEY = "TOKEN_KEY"
        private var INSTANCE: AppAuth? = null


        fun init(context: Context) {
            INSTANCE = AppAuth(context)
        }

        fun getInstance() = requireNotNull(INSTANCE) {
            "need call init() first"
        }
    }


    private val prefs =
        context.applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val _data: MutableStateFlow<Token?>

    init {
        val id = prefs.getLong(ID_KEY, 0L)
        val token = prefs.getString(TOKEN_KEY, null)

        if (id == 0L || token == null) {
            prefs.edit{
                clear()
            }
            _data = MutableStateFlow(null)
        } else {
            _data = MutableStateFlow(Token(id, token))
        }
    }

    val data = _data.asStateFlow()
    val authStateFlow = data



    fun setAuth(id: Long, token: String) {
        prefs.edit {
            putLong(ID_KEY, id)
            putString(TOKEN_KEY, token)
        }
        _data.value = Token(id, token)
    }

    fun removeAuth() {
        prefs.edit {
            clear()
        }
            _data.value = null
        }
}