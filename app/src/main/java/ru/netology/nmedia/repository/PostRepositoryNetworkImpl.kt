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

    // ==================================================
    // Получение новых постов после последнего ID в БД
    // ==================================================
    override fun getNewer(lastSeenId: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)

            try {
                val latestId = dao.getLatestId() ?: 0L
                val posts = PostApi.retrofitService.getNewer(latestId)

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

    // ==================================================
    // Получение всех постов
    // ==================================================
    override suspend fun getAllAsync() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    // ==================================================
    // Удаление поста
    // ==================================================
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

    // ==================================================
    // Лайк / дизлайк
    // ==================================================
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

    // ==================================================
    // Сохранение поста (онлайн + оффлайн)
    // ==================================================
    override suspend fun save(post: Post): Post {
        return try {
            val saved = PostApi.retrofitService.save(post)

            dao.insert(PostEntity.fromDto(saved, isLocal = false))

            saved
        } catch (e: IOException) {
            // сохраняем оффлайн с уникальным большим ID
            val local = post.copy(
                id = System.currentTimeMillis(),
                published = System.currentTimeMillis()
            )

            dao.insert(PostEntity.fromDto(local, isLocal = true))

            local
        }
    }

    // ==================================================
    // Повторная отправка локальных постов
    // ==================================================
    suspend fun retryUnsyncedPosts() {
        val unsynced = dao.getUnsynced()

        for (post in unsynced) {
            try {
                val saved = PostApi.retrofitService.save(post.toDto())
                dao.insert(PostEntity.fromDto(saved, isLocal = false))
            } catch (_: IOException) {
            }
        }
    }

    // ==================================================
    // Вытягивание всех постов (для обратной совместимости)
    // ==================================================
    override suspend fun getAll() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }
}