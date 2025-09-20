package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post

class PostRepositorySharedPrefImpl(context: Context) : PostRepository {

    private val pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
    private var index: Long = 1L
    private var posts: List<Post> = emptyList()
        set(value) {
            field = value
            data.value = posts
            sync()
        }
    private val data = MutableLiveData<List<Post>>(posts)

    init {
        pref.getString(POSTS_KEY, null)?.let { json ->
            posts = gson.fromJson(json, type)
            data.value = posts
            index = (posts.maxOfOrNull { it.id } ?: 0L) + 1
        }
    }

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        posts = posts.map { post ->
            if (post.id == id) {
                post.copy(
                    likeByMe = !post.likeByMe,
                    likes = if (post.likeByMe) post.likes - 1 else post.likes + 1
                )
            } else post
        }
    }

    override fun share(id: Long) {
        posts = posts.map { post ->
            if (post.id == id) post.copy(shares = post.shares + 1) else post
        }
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
    }

    override fun save(post: Post) {
        posts = if (post.id == 0L) {
            listOf(
                post.copy(
                    id = index++,
                    author = "Me",
                    published = "now"
                )
            ) + posts
        } else {
            posts.map { if (it.id == post.id) it.copy(content = post.content) else it }
        }
    }

    private fun sync() {
        pref.edit()
            .putString(POSTS_KEY, gson.toJson(posts, type))
            .apply()
    }

    companion object {
        private const val SHARED_PREF_NAME = "repo"
        private const val POSTS_KEY = "posts"
        private val gson = Gson()
        private val type = object : TypeToken<List<Post>>() {}.type
    }
}