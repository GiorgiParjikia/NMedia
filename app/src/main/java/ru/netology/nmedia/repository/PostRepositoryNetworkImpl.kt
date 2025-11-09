package ru.netology.nmedia.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.AppError
import java.io.IOException

class PostRepositoryNetworkImpl(
    private val dao: PostDao,
) : PostRepository {

    override val data = dao.getAll().map { list ->
        list.map { it.toDto() }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)

            try {
                // API возвращает List<Post>
                val posts = PostApi.retrofitService.getNewer(id)

                if (posts.isNotEmpty()) {
                    dao.insert(posts.map(PostEntity::fromDto))
                    emit(posts.size)
                }

            } catch (e: Exception) {
                throw AppError.from(e)
            }
        }
    }

    override fun isEmpty() = dao.isEmpty()

    override suspend fun getAllAsync() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    override suspend fun removeById(id: Long) {
        val postToRemove = dao.getPostById(id)?.toDto()
        dao.removeById(id)

        try {
            PostApi.retrofitService.deleteById(id)
        } catch (e: IOException) {
            if (postToRemove != null) {
                dao.insert(PostEntity.fromDto(postToRemove))
            }
            throw e
        }
    }

    override suspend fun likeById(id: Long): Post {
        val post = dao.getPostById(id)?.toDto()
            ?: throw RuntimeException("Post not found")

        val liked = !post.likedByMe
        val updated = post.copy(
            likedByMe = liked,
            likes = post.likes + if (liked) 1 else -1
        )

        dao.insert(PostEntity.fromDto(updated))

        return try {
            val result = if (liked) {
                PostApi.retrofitService.likeById(id)
            } else {
                PostApi.retrofitService.dislikeById(id)
            }
            dao.insert(PostEntity.fromDto(result))
            result
        } catch (e: IOException) {
            dao.insert(PostEntity.fromDto(post))
            throw e
        }
    }

    override suspend fun save(post: Post): Post {
        val tmp = PostEntity.fromDto(
            post.copy(id = 0, published = System.currentTimeMillis()),
            isLocal = true
        )

        dao.insert(tmp)

        return try {
            val saved = PostApi.retrofitService.save(post)
            dao.insert(PostEntity.fromDto(saved, isLocal = false))
            saved
        } catch (e: IOException) {
            post
        }
    }

    suspend fun retryUnsyncedPosts() {
        val unsynced = dao.getUnsynced()
        for (post in unsynced) {
            try {
                val saved = PostApi.retrofitService.save(post.toDto())
                dao.insert(PostEntity.fromDto(saved, isLocal = false))
            } catch (_: IOException) {}
        }
    }

    override suspend fun getAll() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }
}