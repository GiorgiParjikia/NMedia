package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: PostsApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val db: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {

        try {
            val response = when (loadType) {

                LoadType.REFRESH -> {
                    val maxId = postDao.getLatestId() ?: 0L
                    if (maxId == 0L)
                        apiService.getLatest(state.config.pageSize)
                    else
                        apiService.getAfter(maxId, state.config.pageSize)
                }

                LoadType.PREPEND ->
                    return MediatorResult.Success(endOfPaginationReached = true)

                LoadType.APPEND -> {
                    val minId = postRemoteKeyDao.min()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)

                    apiService.getBefore(minId, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) throw HttpException(response)

            val posts = response.body().orEmpty()
            val isEnd = posts.isEmpty()

            db.withTransaction {

                when (loadType) {

                    LoadType.REFRESH -> {
                        postRemoteKeyDao.clear()

                        if (posts.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                posts.map {
                                    PostRemoteKeyEntity(
                                        id = it.id,
                                        key = it.id,
                                        type = PostRemoteKeyEntity.KeyType.AFTER
                                    )
                                }
                            )
                        }
                    }

                    LoadType.APPEND -> {
                        if (posts.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                posts.map {
                                    PostRemoteKeyEntity(
                                        id = it.id,
                                        key = it.id,
                                        type = PostRemoteKeyEntity.KeyType.BEFORE
                                    )
                                }
                            )
                        }
                    }

                    LoadType.PREPEND -> Unit
                }

                postDao.insert(posts.map(PostEntity::fromDto))
            }

            return MediatorResult.Success(endOfPaginationReached = isEnd)

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}