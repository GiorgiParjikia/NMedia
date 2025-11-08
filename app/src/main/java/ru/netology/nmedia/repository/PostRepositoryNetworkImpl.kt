package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryNetworkImpl(
    private val dao: PostDao,
) : PostRepository {

    // Преобразуем данные из БД в DTO
    override val data: LiveData<List<Post>>
        get() = dao.getAll().map { entities ->
            entities.map(PostEntity::toDto)
        }

    override fun isEmpty() = dao.isEmpty()

    // Получаем все посты с сервера, сохраняем в БД, возвращаем список
    override suspend fun getAllAsync() {
        val posts = PostApi.retrofitService.getAll()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    override suspend fun getAll() {
        TODO("Not yet implemented")
    }

    override suspend fun likeById(id: Long): Post {
        TODO("Not yet implemented")
    }

    override suspend fun removeById(id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun save(post: Post): Post {
       val postFromServer = PostApi.retrofitService.save(post)
        dao.insert(PostEntity.fromDto(postFromServer))
        return postFromServer
    }
}