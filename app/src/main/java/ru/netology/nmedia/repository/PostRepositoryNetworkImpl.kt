package ru.netology.nmedia.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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
    override fun getAll(): List<Post> = emptyList() // не используется больше

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) throw IOException("GET /api/posts failed: ${it.code}")
                        val body = it.body?.string() ?: throw IOException("Empty body")
                        val posts = gson.fromJson<List<Post>>(body, listType)
                        callback.onSuccess(posts)
                    }
                } catch (e: Exception) {
                    callback.onError(e)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e)
            }
        })
    }

    // ====== 2. Лайк / дизлайк ======
    override fun likeById(id: Long, likedByMe: Boolean): Post {
        val method = if (likedByMe) "DELETE" else "POST"
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id/likes")
            .method(method, if (likedByMe) null else "".toRequestBody())
            .build()

        Log.i("NETWORK", "$method /api/posts/$id/likes")

        // этот метод возвращает результат сразу (используем execute)
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("$method /api/posts/$id/likes failed: ${response.code}")
            val bodyString = response.body?.string()
                ?: throw IOException("Response body is null on likeById()")
            return gson.fromJson(bodyString, Post::class.java)
        }
    }

    // ====== 3. Удалить пост (enqueue) ======
    override fun removeById(id: Long) {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("NETWORK", "DELETE /api/posts/$id failed: ${it.code}")
                    } else {
                        Log.i("NETWORK", "Post $id deleted successfully")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("NETWORK", "DELETE /api/posts/$id failed", e)
            }
        })
    }

    // ====== 4. Сохранить пост (enqueue) ======
    override fun save(post: Post): Post {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .post(gson.toJson(post).toRequestBody(jsonType))
            .build()

        // Этот метод по контракту возвращает Post — оставляем execute
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("POST /api/posts failed: ${response.code}")
            val body = response.body?.string() ?: throw IOException("Response body is null on save()")
            return gson.fromJson(body, Post::class.java)
        }
    }
}