package ru.netology.nmedia.viewmodel

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Token
import javax.inject.Inject

data class AuthState(
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val api: PostsApiService,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _state = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = _state

    private val _authSuccess = MutableLiveData<Unit>()
    val authSuccess: LiveData<Unit> = _authSuccess

    fun signIn(login: String, pass: String) {
        viewModelScope.launch {
            try {
                _state.value = AuthState(loading = true)

                val response = api.authenticate(login, pass)

                if (!response.isSuccessful) {
                    _state.value = AuthState(
                        error = "Ошибка авторизации: ${response.code()}"
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