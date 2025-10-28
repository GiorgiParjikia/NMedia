package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dto.Post
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class PostRepositoryNetworkImpl : PostRepository {

    // LiveData, которую будет наблюдать UI
    private val data = MutableLiveData<List<Post>>(emptyList())

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val listType = object : TypeToken<List<Post>>() {}.type

    companion object {
        // Для эмулятора: обращаемся к локальной машине через 10.0.2.2
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val JSON = "application/json".toMediaType()
    }

    // 1. дать UI поток данных
    override fun get(): LiveData<List<Post>> = data

    // 2. Лайк / дизлайк
    override fun like(id: Long) {
        thread {
            try {
                val current = data.value ?: emptyList()
                val target = current.firstOrNull { it.id == id } ?: return@thread

                val updatedPost = if (!target.likeByMe) {
                    doLikeRequest(id)
                } else {
                    doUnlikeRequest(id)
                }

                val newList = current.map { post ->
                    if (post.id == id) updatedPost else post
                }
                data.postValue(newList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun doLikeRequest(id: Long): Post {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id/likes")
            .post("".toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("POST /likes failed: ${response.code}")
            }
            val bodyString = response.body?.string()
                ?: throw RuntimeException("body is null")
            return gson.fromJson(bodyString, Post::class.java)
        }
    }

    private fun doUnlikeRequest(id: Long): Post {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts/$id/likes")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DELETE /likes failed: ${response.code}")
            }
            val bodyString = response.body?.string()
                ?: throw RuntimeException("body is null")
            return gson.fromJson(bodyString, Post::class.java)
        }
    }

    // 3. share / removeById / save — временно заглушки, чтобы проект собирался
    // (курс сейчас требует лайки, а эти вещи пойдут позже через сеть)

    override fun share(id: Long) {
        // TODO: реализовать через сервер позже
    }

    override fun removeById(id: Long) {
        // TODO: реализовать через сервер позже
    }

    override fun save(post: Post) {
        // TODO: реализовать сохранение через сервер позже
    }

    // 4. Это НЕ в интерфейсе, поэтому приватно для VM:
    fun refresh() {
        thread {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/posts")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("GET /api/posts failed: ${response.code}")
                    }

                    val bodyString = response.body?.string()
                        ?: throw RuntimeException("body is null")

                    val posts: List<Post> = gson.fromJson(bodyString, listType)
                    data.postValue(posts)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
