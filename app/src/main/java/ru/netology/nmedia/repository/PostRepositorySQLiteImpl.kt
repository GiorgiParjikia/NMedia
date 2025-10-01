/*package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post


class PostRepositorySQLiteImpl(
    private val dao: PostDao
) : PostRepository {

    private val data = MutableLiveData(dao.getAll())

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        dao.likeById(id)
        sync()
    }

    override fun share(id: Long) {
        dao.shareById(id)
        sync()
    }

    override fun removeById(id: Long) {
        dao.removeById(id)
        sync()
    }

    override fun save(post: Post) {
        dao.save(post)
        sync()
    }

    private fun sync() {
        data.value = dao.getAll()
    }
}
 */