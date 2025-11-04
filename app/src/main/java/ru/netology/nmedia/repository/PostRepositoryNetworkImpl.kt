package ru.netology.nmedia.repository

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post
import java.io.IOException

class PostRepositoryNetworkImpl : PostRepository {

    interface NetworkCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Throwable)
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        PostApi.service.getAll().enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (!response.isSuccessful) {
                    val errorMsg = "Ошибка загрузки: ${response.code()} ${response.message()}"
                    Log.e("NETWORK", errorMsg)
                    callback.onError(IOException(errorMsg))
                    return
                }

                val posts = response.body()
                if (posts == null) {
                    callback.onError(IOException("Пустое тело ответа"))
                    return
                }
                callback.onSuccess(posts)
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                Log.e("NETWORK", "Ошибка сети: ${t.message}", t)
                callback.onError(t)
            }
        })
    }

    fun likeByIdAsync(id: Long, likedByMe: Boolean, callback: NetworkCallback<Post>) {
        val call = if (likedByMe) PostApi.service.dislikeById(id) else PostApi.service.likeById(id)
        call.enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    response.body()?.let { callback.onSuccess(it) }
                        ?: callback.onError(IOException("Пустое тело при likeByIdAsync"))
                } else {
                    callback.onError(IOException("Ошибка при лайке: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    fun removeByIdAsync(id: Long, callback: NetworkCallback<Unit>) {
        PostApi.service.deleteById(id).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    callback.onSuccess(Unit)
                } else {
                    callback.onError(IOException("Ошибка удаления: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    fun saveAsync(post: Post, callback: NetworkCallback<Post>) {
        PostApi.service.save(post).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    response.body()?.let { callback.onSuccess(it) }
                        ?: callback.onError(IOException("Пустое тело при saveAsync"))
                } else {
                    callback.onError(IOException("Ошибка сохранения: ${response.code()}"))
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                callback.onError(t)
            }
        })
    }

    override fun getAll(): List<Post> = emptyList()
    override fun likeById(id: Long, likedByMe: Boolean): Post = throw NotImplementedError()
    override fun removeById(id: Long) = Unit
    override fun save(post: Post): Post = throw NotImplementedError()
}