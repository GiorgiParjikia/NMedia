package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post
import java.io.File

class PostRepositoryFileImpl(
    private val context: Context
) : PostRepository {

    private var nextId: Long = 1L

    private var posts: List<Post> = emptyList()
        set(value) {
            field = value
            data.value = value
            sync()
        }

    private val data = MutableLiveData<List<Post>>(posts)

    init {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            runCatching {
                file.bufferedReader().use { reader ->
                    gson.fromJson<List<Post>>(reader, type)
                }
            }.onSuccess { loaded ->
                posts = loaded ?: emptyList()
                nextId = (posts.maxOfOrNull { it.id } ?: 0L) + 1
            }.onFailure {
                posts = emptyList()
                nextId = 1L
                sync()
            }
        } else {
            sync()
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
                    id = nextId++,
                    author = "Me",
                    published = "now"
                )
            ) + posts
        } else {
            posts.map { if (it.id == post.id) it.copy(content = post.content) else it }
        }
    }

    private fun sync() {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)
            .bufferedWriter()
            .use { writer ->
                writer.write(gson.toJson(posts, type))
            }
    }

    companion object {
        private const val FILE_NAME = "posts.json"
        private val gson = Gson()
        private val type = object : TypeToken<List<Post>>() {}.type
    }
}