package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val api: PostsApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _state = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = _state

    private val _authSuccess = MutableLiveData<Unit>()
    val authSuccess: LiveData<Unit> = _authSuccess

    fun signUp(name: String, login: String, pass: String, passConfirm: String) {
        if (pass != passConfirm) {
            _state.value = AuthState(error = "Пароли не совпадают")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = AuthState(loading = true)

                val response = api.register(login, pass, name)

                if (!response.isSuccessful) {
                    _state.value = AuthState(
                        error = "Ошибка регистрации: ${response.code()}"
                    )
                    return@launch
                }

                val token: Token = response.body()
                    ?: run {
                        _state.value = AuthState(error = "Пустой ответ сервера")
                        return@launch
                    }

                appAuth.setAuth(token.id, token.token)

                _state.value = AuthState()
                _authSuccess.value = Unit

            } catch (e: Exception) {
                _state.value = AuthState(error = e.message ?: "Неизвестная ошибка")
            }
        }
    }
}
