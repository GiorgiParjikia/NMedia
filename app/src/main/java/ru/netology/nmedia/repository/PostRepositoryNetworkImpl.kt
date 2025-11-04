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
            throw RuntimeException(response.errorBody()?.string() ?: "Response is not successful")
        }
        return response.body() ?: throw RuntimeException("Body is null")
    }

    override fun getAllAsync(callback: PostRepository.GetAllCallback) {
        PostApi.service.getAll()
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                    if (!response.isSuccessful) {
                        callback.onError(RuntimeException(response.errorBody()?.string()))
                        return
                    }
                    val posts = response.body()
                    if (posts == null) {
                        callback.onError(RuntimeException("Body is null"))
                        return
                    }
                    callback.onSuccess(posts)
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                    callback.onError(t)
                }
            })
    }

    override fun likeById(id: Long, likedByMe: Boolean): Post {
        val call = if (likedByMe) {
            PostApi.service.dislikeById(id)
        } else {
            PostApi.service.likeById(id)
        }

        val response = call.execute()
        if (!response.isSuccessful) {
            throw IOException("likeById failed: ${response.code()}")
        }
        return response.body() ?: throw IOException("Response body is null on likeById()")
    }

    override fun removeById(id: Long) {
        val response = PostApi.service.deleteById(id).execute()
        if (!response.isSuccessful) {
            Log.e("NETWORK", "DELETE /api/posts/$id failed: ${response.code()}")
            throw IOException("Delete failed: ${response.code()}")
        } else {
            Log.i("NETWORK", "Post $id deleted successfully")
        }
    }

    override fun save(post: Post): Post {
        val response = PostApi.service.save(post).execute()
        if (!response.isSuccessful) {
            throw IOException("POST /api/posts failed: ${response.code()}")
        }
        return response.body() ?: throw IOException("Response body is null on save()")
    }
}