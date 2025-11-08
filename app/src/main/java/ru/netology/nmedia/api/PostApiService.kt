package ru.netology.nmedia.api

import ru.netology.nmedia.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

private const val BASE_URL = "${BuildConfig.BASE_URL}/api/slow/"

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
        }
    }
    .build()

private val retrofit = Retrofit.Builder()
    .client(client)
    .baseUrl(BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

interface PostApiService {

    // ====== Получение всех постов ======
    @GET("posts")
    suspend fun getAll(): List<Post>

    // ====== Сохранение ======
    @POST("posts")
    suspend fun save(@Body post: Post): Post

    // ====== Удаление ======
    @DELETE("posts/{id}")
    suspend fun deleteById(@Path("id") id: Long)

    // ====== Лайк ======
    @POST("posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Post

    // ====== Дизлайк ======
    @DELETE("posts/{id}/likes")
    suspend fun dislikeById(@Path("id") id: Long): Post
}

object PostApi {
    val retrofitService: PostApiService by lazy {
        retrofit.create(PostApiService::class.java)
    }
}