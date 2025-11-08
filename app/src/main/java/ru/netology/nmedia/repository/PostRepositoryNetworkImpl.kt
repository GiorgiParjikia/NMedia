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

    // 🔹 Получение постов с сервера и сохранение в БД
    override suspend fun getAllAsync() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    // 🔹 Удаление поста
    override suspend fun removeById(id: Long) {
        val postToRemove = dao.getAll().value?.find { it.id == id }?.toDto()
        dao.removeById(id)
        try {
            PostApi.retrofitService.deleteById(id)
        } catch (e: IOException) {
            // откатываем локальные изменения при ошибке
            if (postToRemove != null) {
                dao.insert(PostEntity.fromDto(postToRemove))
            }
            throw e
        }
    }

    // 🔹 Лайк / дизлайк
    override suspend fun likeById(id: Long): Post {
        // Берём пост напрямую из БД
        val post = dao.getPostById(id)?.toDto()
            ?: throw RuntimeException("Post not found")

        // Меняем локально лайк
        val liked = !post.likedByMe
        val updated = post.copy(
            likedByMe = liked,
            likes = post.likes + if (liked) 1 else -1
        )
        dao.insert(PostEntity.fromDto(updated)) // UI сразу обновится

        return try {
            // Отправляем запрос на сервер
            val response = if (liked) {
                PostApi.retrofitService.likeById(id)
            } else {
                PostApi.retrofitService.dislikeById(id)
            }

            // Обновляем в БД ответ сервера (вдруг там изменились другие поля)
            dao.insert(PostEntity.fromDto(response))
            response
        } catch (e: Exception) {
            // В случае ошибки откатываем
            dao.insert(PostEntity.fromDto(post))
            throw e
        }
    }


    override suspend fun getAll() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    override suspend fun save(post: Post): Post {
        val postFromServer = PostApi.retrofitService.save(post)
        dao.insert(PostEntity.fromDto(postFromServer))
        return postFromServer
    }
}