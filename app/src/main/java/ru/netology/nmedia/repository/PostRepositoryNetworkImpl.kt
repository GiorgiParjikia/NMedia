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

    // ===== Лайк / дизлайк (упрощённая версия) =====
    override suspend fun likeById(id: Long): Post {
        // обновляем локально
        dao.likeById(id)

        return try {
            val response = if (dao.getPostById(id)?.likedByMe == false) {
                PostApi.retrofitService.likeById(id)
            } else {
                PostApi.retrofitService.dislikeById(id)
            }
            response
        } catch (e: IOException) {
            // при ошибке сети откатываем изменения
            dao.likeById(id)
            throw e
        }
    }

    // ===== Сохранение (оптимистичное) =====
    override suspend fun save(post: Post): Post {
        // 1 сохраняем локально (с нулевым id, чтобы сервер создал новый)
        val localEntity = PostEntity.fromDto(
            post.copy(
                id = 0,
                published = System.currentTimeMillis()
            ),
            isLocal = true
        )
        dao.insert(localEntity)

        // 2 пытаемся отправить на сервер
        return try {
            val saved = PostApi.retrofitService.save(post)
            dao.removeById(localEntity.id) // удаляем локальную копию
            dao.insert(PostEntity.fromDto(saved, isLocal = false))
            saved
        } catch (e: IOException) {
            // 3 сеть недоступна — просто остаётся в локальной БД
            e.printStackTrace()
            post
        }
    }

    // ===== Повторная отправка несинхронизированных постов =====
    suspend fun retryUnsyncedPosts() {
        val unsynced = dao.getUnsynced()
        for (entity in unsynced) {
            try {
                val dto = entity.toDto().copy(id = 0) // id назначит сервер
                val response = PostApi.retrofitService.save(dto)
                dao.removeById(entity.id) // удаляем локальную копию
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