package ru.netology.nmedia.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
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
        private const val BASE_URL = "http://10.0.2.2:9999"
    }

    // ====== 1. Получить все посты ======
    override fun getAll(): List<Post> {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("GET /api/posts failed: ${response.code}")
            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null")
            return gson.fromJson(bodyString, listType)
        }
    }

    // ====== 2. Лайк / дизлайк ======
    override fun likeById(id: Long, likedByMe: Boolean): Post {
        val method = if (likedByMe) "DELETE" else "POST"

        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id/likes")
            .method(method, if (likedByMe) null else "".toRequestBody())
            .build()

        Log.i("NETWORK", "$method /api/posts/$id/likes")

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("$method /api/posts/$id/likes failed: ${response.code}")

            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null on likeById()")

            Log.i("NETWORK", "Response body: $bodyString")
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
            if (!response.isSuccessful) throw IOException("DELETE /api/posts/$id failed: ${response.code}")
        }
    }

    // ====== 4. Сохранить пост ======
    override fun save(post: Post): Post {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .post(gson.toJson(post).toRequestBody(jsonType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("POST /api/posts failed: ${response.code}")

            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null on save()")

            return gson.fromJson(bodyString, Post::class.java)
        }
    }
}