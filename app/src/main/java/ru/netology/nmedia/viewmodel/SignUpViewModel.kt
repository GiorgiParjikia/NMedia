// ru/netology/nmedia/viewmodel/SignUpViewModel.kt
package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token

class SignUpViewModel : ViewModel() {

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

                val response = PostsApi.service.register(login, pass, name)

                if (!response.isSuccessful) {
                    _state.value = AuthState(error = "Ошибка регистрации: ${response.code()}")
                    return@launch
                }

                val token: Token = response.body()
                    ?: run {
                        _state.value = AuthState(error = "Пустой ответ сервера")
                        return@launch
                    }

                AppAuth.getInstance().setAuth(token.id, token.token)
                _state.value = AuthState()
                _authSuccess.value = Unit

            } catch (e: Exception) {
                _state.value = AuthState(error = e.message ?: "Неизвестная ошибка")
            }
        }
    }
}