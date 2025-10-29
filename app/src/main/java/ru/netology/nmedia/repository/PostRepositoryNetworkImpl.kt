package ru.netology.nmedia.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.PostCreateRequest
import java.io.IOException
import java.util.concurrent.TimeUnit

class PostRepositoryNetworkImpl : PostRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()
    private val listType = object : TypeToken<List<Post>>() {}.type

    companion object {
        // На эмуляторе 10.0.2.2 = localhost хоста
        private const val BASE_URL = "http://10.0.2.2:9999"
    }

    // ====== 1. Получить все посты ======
    override fun getAll(): List<Post> {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GET /api/posts failed: ${response.code}")
            }

            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null")

            return gson.fromJson(bodyString, listType)
        }
    }

    // ====== 2. Лайк / дизлайк ======
    override fun likeById(id: Long): Post {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id/likes")
            .post("".toRequestBody()) // тело пустое
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("POST /posts/$id/likes failed: ${response.code}")
            }

            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null on likeById()")

            return gson.fromJson(bodyString, Post::class.java)
        }
    }

    // ====== 3. Удалить пост ======
    override fun removeById(id: Long) {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DELETE /api/posts/$id failed: ${response.code}")
            }
        }
        // ничего не возвращаем
    }

    // ====== 4. Сохранить пост (новый или редактированный) ======
    override fun save(post: Post): Post {
        // 🔧 Формируем тело запроса так, как ожидает сервер
        val payload = PostCreateRequest(
            id = if (post.id == 0L) 0L else post.id,
            author = post.author,
            content = post.content,
            published = 0L, // сервер сам проставит актуальное время
            likedByMe = post.likeByMe,
            likes = post.likes.toInt()
        )

        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .post(
                gson.toJson(payload)
                    .toRequestBody(jsonType)
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("POST /api/posts failed: ${response.code}")
            }

            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null on save()")

            return gson.fromJson(bodyString, Post::class.java)
        }
    }
}