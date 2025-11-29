package ru.netology.nmedia.repository

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.*
import ru.netology.nmedia.entity.PostEntity
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random

class PostRepositoryNetworkImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: PostsApiService,
    private val remoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
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
            keyDao = remoteKeyDao,
            db = appDb
        ),
        pagingSourceFactory = dao::pagingSource
    )
        .flow
        .map { pagingData ->
            pagingData
                .map(PostEntity::toDto)
                .insertSeparators { before, after ->

                    // 🔹 Нет разделителя
                    if (before == null || after == null) return@insertSeparators null

                    val dateBefore = before.published
                    val dateAfter = after.published

                    // 🔹 Сегодня — Вчера — Неделя
                    val diff = dateBefore - dateAfter

                    when {
                        diff < 24 * 60 * 60 * 1000 -> FeedItem.TodaySeparator("Сегодня")
                        diff < 2 * 24 * 60 * 60 * 1000 -> FeedItem.YesterdaySeparator("Вчера")
                        diff < 7 * 24 * 60 * 60 * 1000 -> FeedItem.LastWeekSeparator("На прошлой неделе")

                        else -> null
                    }
                }
                .map { feed ->

                    // 🔹 Вставляем рекламу каждые 5 элементов
                    if (Random.nextInt(0, 5) == 0) {
                        Ad(Random.nextLong(), "https://netology.ru")
                    } else feed
                }
        }

    override fun isEmpty(): Flow<Boolean> = dao.isEmpty()

    override suspend fun revealHidden() {
        dao.revealHiddenPosts()
    }

    override suspend fun removeById(id: Long) {
        val old = dao.getPostById(id)?.toDto()
        dao.removeById(id)

        try {
            apiService.removeById(id)
        } catch (e: IOException) {
            if (old != null) dao.insert(PostEntity.fromDto(old))
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
            val response = if (liked) apiService.likeById(id) else apiService.dislikeById(id)
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
                file.asRequestBody()
            )
        )
        return response.body() ?: throw RuntimeException("Upload failed")
    }
}