package ru.netology.nmedia.repository

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post
import java.io.IOException

class PostRepositoryNetworkImpl : PostRepository {

    override fun getAll(): List<Post> {
        val response = PostApi.service.getAll().execute()
        if (!response.isSuccessful) {
            throw IOException("Server error: ${response.code()} ${response.message()}")
        }
        return response.body() ?: throw IOException("Response body is null")
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

    override fun likeById(id: Long, likedByMe: Boolean): Post {
        val call = if (likedByMe) PostApi.service.dislikeById(id) else PostApi.service.likeById(id)
        val response = call.execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка при лайке: ${response.code()} ${response.message()}")
        }
        return response.body() ?: throw IOException("Пустой ответ при likeById()")
    }

    override fun removeById(id: Long) {
        val response = PostApi.service.deleteById(id).execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка удаления: ${response.code()} ${response.message()}")
        }
    }

    override fun save(post: Post): Post {
        val response = PostApi.service.save(post).execute()
        if (!response.isSuccessful) {
            throw IOException("Ошибка сохранения: ${response.code()} ${response.message()}")
        }
        return response.body() ?: throw IOException("Пустой ответ при save()")
    }
}