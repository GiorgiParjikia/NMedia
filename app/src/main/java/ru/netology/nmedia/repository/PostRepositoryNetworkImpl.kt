package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random

class PostRepositoryNetworkImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: PostsApiService,
    postRemoteKeyDao: PostRemoteKeyDao,
    appDb: AppDb,
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<FeedItem>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false
        ),
        remoteMediator = PostRemoteMediator(
            api = apiService,
            postDao = dao,
            keyDao = postRemoteKeyDao,
            db = appDb
        ),
        pagingSourceFactory = dao::pagingSource
    )
        .flow
        .map { pagingData ->
            pagingData.map(PostEntity::toDto)
                .insertSeparators { _, _ -> null }

        }

    override fun isEmpty(): Flow<Boolean> = dao.isEmpty()

    override suspend fun revealHidden() {
        dao.revealHiddenPosts()
    }

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
            val response =
                if (liked) apiService.likeById(id) else apiService.dislikeById(id)

            val result = response.body() ?: updated
            dao.insert(PostEntity.fromDto(result))
            result
        } catch (e: IOException) {
            dao.insert(PostEntity.fromDto(post))
            throw e
        }
    }

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
            dao.insert(PostEntity.fromDto(saved))
            saved

        } catch (e: IOException) {
            val local = post.copy(
                id = -System.currentTimeMillis(),
                published = 0L
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
}