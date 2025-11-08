package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.io.IOException

class PostRepositoryNetworkImpl(
    private val dao: PostDao,
) : PostRepository {

    override val data: LiveData<List<Post>>
        get() = dao.getAll().map { entities ->
            entities.map(PostEntity::toDto)
        }

    override fun isEmpty() = dao.isEmpty()

    // ===== Получение всех постов =====
    override suspend fun getAllAsync() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    // ===== Удаление =====
    override suspend fun removeById(id: Long) {
        val postToRemove = dao.getPostById(id)?.toDto()
        dao.removeById(id)
        try {
            PostApi.retrofitService.deleteById(id)
        } catch (e: IOException) {
            // откат, если не удалось удалить на сервере
            if (postToRemove != null) {
                dao.insert(PostEntity.fromDto(postToRemove))
            }
            throw e
        }
    }

    // ===== Лайк / дизлайк =====
    override suspend fun likeById(id: Long): Post {
        val post = dao.getPostById(id)?.toDto()
            ?: throw RuntimeException("Post not found")

        val liked = !post.likedByMe
        val updated = post.copy(
            likedByMe = liked,
            likes = post.likes + if (liked) 1 else -1
        )

        dao.insert(PostEntity.fromDto(updated)) // обновляем локально

        return try {
            val response = if (liked) {
                PostApi.retrofitService.likeById(id)
            } else {
                PostApi.retrofitService.dislikeById(id)
            }
            dao.insert(PostEntity.fromDto(response))
            response
        } catch (e: IOException) {
            // при ошибке сети откатываем изменения
            dao.insert(PostEntity.fromDto(post))
            throw e
        }
    }

    // ===== Сохранение (оптимистичное) =====
    override suspend fun save(post: Post): Post {
        // 1️⃣ сохраняем локально, чтобы сразу увидеть пост
        val localEntity = PostEntity.fromDto(
            post.copy(
                id = 0,
                published = System.currentTimeMillis()
            ),
            isLocal = true
        )
        dao.insert(localEntity)

        // 2️⃣ пытаемся отправить на сервер
        return try {
            val saved = PostApi.retrofitService.save(post)
            dao.insert(PostEntity.fromDto(saved, isLocal = false))
            saved
        } catch (e: IOException) {
            // 3️⃣ сеть недоступна — просто остаётся в локальной БД
            e.printStackTrace()
            post
        }
    }

    // ===== Повторная отправка несинхронизированных постов =====
    suspend fun retryUnsyncedPosts() {
        val unsynced = dao.getUnsynced()
        for (entity in unsynced) {
            try {
                val response = PostApi.retrofitService.save(entity.toDto())
                dao.insert(PostEntity.fromDto(response, isLocal = false))
            } catch (_: IOException) {
                // сети всё ещё нет — пропускаем
            }
        }
    }

    override suspend fun getAll() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }
}