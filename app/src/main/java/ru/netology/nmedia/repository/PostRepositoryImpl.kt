package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity

class PostRepositoryImpl(
    private val dao: PostDao,
) : PostRepository {

    override fun get(): LiveData<List<Post>> =
        dao.getAll().map { list -> list.toDto() }

    override fun like(id: Long) {
        dao.likeById(id)
    }

    override fun share(id: Long) {
        dao.shareById(id)
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
    }

    override fun save(post: Post) {
        dao.save(post.toEntity())
    }
}
