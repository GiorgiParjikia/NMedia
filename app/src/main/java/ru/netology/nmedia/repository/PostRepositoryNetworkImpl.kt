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

    // LiveData, которую будет наблюдать UI (актуальный кэш постов)
    private val data = MutableLiveData<List<Post>>(emptyList())

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val listType = object : TypeToken<List<Post>>() {}.type

    companion object {
        // На эмуляторе 10.0.2.2 = localhost хоста
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val JSON = "application/json".toMediaType()
    }

    // 1. отдать LiveData наружу
    override fun get(): LiveData<List<Post>> = data

    // 2. лайк / дизлайк
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

    // 3. share — локальная заглушка (счётчик шеров пока не уходит на сервер)
    override fun share(id: Long) {
        // Можно ничего не делать, UI сам запускает системный share-Intent
    }

    // 4. удалить пост на сервере и обновить локальный список
    override fun removeById(id: Long) {
        thread {
            // сначала оптимистично выпилим в памяти
            val old = data.value.orEmpty()
            val without = old.filter { it.id != id }
            data.postValue(without)

            try {
                val request = Request.Builder()
                    .url("$BASE_URL/api/posts/$id")
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("DELETE /api/posts/$id failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // если ошибка — вернём старый список
                data.postValue(old)
            }
        }
    }

    // 5. сохранить пост (новый или отредактированный)
    override fun save(post: Post) {
        thread {
            try {
                val request = Request.Builder()
                    // медленная ручка из презентации/разбора: /api/slow/posts
                    // если у тебя бэкенд без /slow, замени на /api/posts
                    .url("$BASE_URL/api/slow/posts")
                    .post(
                        gson.toJson(post)
                            .toRequestBody(JSON)
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("POST /api/slow/posts failed: ${response.code}")
                    }

                    val bodyString = response.body?.string()
                        ?: throw RuntimeException("Response body is null on save()")

                    // сервер вернёт пост с настоящим id
                    val savedPost = gson.fromJson(bodyString, Post::class.java)

                    // кладём новый пост в начало списка
                    val current = data.value.orEmpty()
                    val newList = listOf(savedPost) + current
                    data.postValue(newList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // если упало — ничего не меняем, data остаётся прежним
            }
        }
    }

    // 6. асинхронная фоновая загрузка (используется, например, при старте)
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
                // в случае ошибки просто не трогаем data
            }
        }
    }

    // 7. синхронная (блокирующая) загрузка — нужна ViewModel.loadPosts()
    // она вызывается внутри thread { ... }, так что блокировать можно
    fun getAllBlocking(): List<Post> {
        val request = Request.Builder()
            .url("$BASE_URL/api/posts")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GET /api/posts failed: ${response.code}")
            }

            val bodyString = response.body?.string()
                ?: throw RuntimeException("Response body is null")

            val posts: List<Post> = gson.fromJson(bodyString, listType)

            // синхронизируем кэш liveData с тем, что получили
            data.postValue(posts)

            return posts
        }
    }
}