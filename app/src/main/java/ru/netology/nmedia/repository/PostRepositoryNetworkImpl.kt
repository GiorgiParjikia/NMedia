package ru.netology.nmedia.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.error.AppError
import java.io.File
import java.io.IOException
import javax.inject.Inject

class PostRepositoryNetworkImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: PostsApiService,
) : PostRepository {

    override val data = Pager(
        PagingConfig(pageSize = 10, enablePlaceholders = false)
    ) {
        dao.pagingSource()
    }.flow.map { pagingData ->
        pagingData.map { it.toDto() }
    }


    // ----------- New Posts Counter ----------
    override fun getNewer(lastSeenId: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)
            try {
                val latestId = dao.getLatestId() ?: 0L
                val response = apiService.getNewer(latestId)
                val posts = response.body() ?: emptyList()

                if (posts.isNotEmpty()) {
                    dao.insert(
                        posts.map { dto ->
                            PostEntity.fromDto(
                                dto,
                                isLocal = false,
                                localId = null
                            ).copy(isHidden = true)
                        }
                    )
                    emit(posts.size)
                }
            } catch (e: Exception) {
                throw AppError.from(e)
            }
        }
    }

    override fun isEmpty() = dao.isEmpty()

    override suspend fun revealHidden() {
        dao.revealHiddenPosts()
    }

    // --------------- REFRESH (login/logout) ----------------
    override suspend fun refresh() {
        getAllAsync()
    }

    // ----------- Load ALL posts from server -----------
    override suspend fun getAllAsync() {
        val response = apiService.getAll()
        val posts = response.body() ?: emptyList()

        dao.insert(
            posts.map { dto ->
                PostEntity.fromDto(dto).copy(isHidden = false)
            }
        )
    }

    // ----------- Remove -----------
    override suspend fun removeById(id: Long) {
        val postToRemove = dao.getPostById(id)?.toDto()
        dao.removeById(id)

        try {
            apiService.removeById(id)
        } catch (e: IOException) {
            if (postToRemove != null) {
                dao.insert(PostEntity.fromDto(postToRemove))
            }
            throw e
        }
    }

    // ----------- Like -----------
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
            val response = if (liked) apiService.likeById(id) else apiService.dislikeById(id)
            val result = response.body() ?: updated
            dao.insert(PostEntity.fromDto(result))
            result
        } catch (e: IOException) {
            dao.insert(PostEntity.fromDto(post))
            throw e
        }
    }

    // ----------- Save -----------
    override suspend fun save(post: Post, photo: File?): Post {
        return try {
            val media = photo?.let { upload(it) }

            val modified = if (media != null) {
                post.copy(
                    attachment = Attachment(
                        url = media.id,
                        type = AttachmentType.IMAGE
                    )
                )
            } else post

            val response = apiService.save(modified)
            val saved = response.body() ?: modified

            dao.insert(PostEntity.fromDto(saved, isLocal = false))
            saved

        } catch (e: IOException) {
            val local = post.copy(
                id = System.currentTimeMillis(),
                published = System.currentTimeMillis()
            )
            dao.insert(PostEntity.fromDto(local, isLocal = true))
            local
        }
    }

    private suspend fun upload(file: File): Media {
        val response = apiService.upload(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody(),
            )
        )
        return response.body() ?: throw RuntimeException("Upload failed")
    }

    suspend fun retryUnsyncedPosts() {
        val unsynced = dao.getUnsynced()
        for (post in unsynced) {
            try {
                val response = apiService.save(post.toDto())
                val saved = response.body() ?: continue
                dao.insert(PostEntity.fromDto(saved, isLocal = false))
            } catch (_: IOException) { }
        }
    }

    override suspend fun getAll() {
        val response = apiService.getAll()
        val posts = response.body() ?: emptyList()
        dao.insert(posts.map { PostEntity.fromDto(it) })
    }
}
