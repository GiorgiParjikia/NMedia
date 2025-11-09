package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: Flow<List<Post>>

    fun getNewer(id: Long): Flow<Int>

    fun isEmpty(): Flow<Boolean>

    suspend fun getAllAsync()
    suspend fun getAll()
    suspend fun likeById(id: Long) : Post
    suspend fun removeById(id: Long)
    suspend fun save(post: Post): Post
}