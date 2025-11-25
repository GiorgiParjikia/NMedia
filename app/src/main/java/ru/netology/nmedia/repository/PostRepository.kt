package ru.netology.nmedia.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    val data: Flow<PagingData<Post>>

    fun getNewer(id: Long): Flow<Int>

    fun isEmpty(): Flow<Boolean>

    suspend fun getAllAsync()
    suspend fun getAll()

    suspend fun likeById(id: Long): Post
    suspend fun removeById(id: Long)
    suspend fun save(post: Post, photo: File? = null): Post

    suspend fun revealHidden()

    suspend fun refresh()
}
