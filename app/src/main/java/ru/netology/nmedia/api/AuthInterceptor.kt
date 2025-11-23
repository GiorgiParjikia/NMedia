package ru.netology.nmedia.api

import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.Response
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appAuth: Lazy<AppAuth>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = appAuth.get().authStateFlow.value?.token

        val request = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            request.addHeader("Authorization", token)
        }

        return chain.proceed(request.build())
    }
}
