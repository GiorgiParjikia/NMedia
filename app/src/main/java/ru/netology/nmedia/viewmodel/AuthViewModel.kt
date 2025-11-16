package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel : ViewModel() {

    val data = AppAuth.getInstance().data.asLiveData()

    val isAuthenticated: Boolean
        get() = AppAuth.getInstance().data.value != null
}