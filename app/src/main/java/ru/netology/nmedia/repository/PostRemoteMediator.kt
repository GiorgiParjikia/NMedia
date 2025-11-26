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
import ru.netology.nmedia.entity.PostRemoteKeyEntity.KeyType

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
                    apiService.getLatest(state.config.pageSize)
                }

                LoadType.PREPEND -> {
                    val maxId = postRemoteKeyDao.getKey(KeyType.AFTER)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)

                    apiService.getAfter(maxId, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val minId = postRemoteKeyDao.getKey(KeyType.BEFORE)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)

                    apiService.getBefore(minId, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) throw HttpException(response)

            val posts = response.body().orEmpty()
            val isEnd = posts.isEmpty()

            db.withTransaction {

                when (loadType) {

                    LoadType.REFRESH -> {
                        postRemoteKeyDao.delete(KeyType.AFTER)
                        postRemoteKeyDao.delete(KeyType.BEFORE)

                        if (posts.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = KeyType.AFTER,
                                    id = posts.first().id
                                )
                            )
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = KeyType.BEFORE,
                                    id = posts.last().id
                                )
                            )
                        }
                    }

                    LoadType.PREPEND -> {
                        if (posts.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = KeyType.AFTER,
                                    id = posts.first().id
                                )
                            )
                        }
                    }

                    LoadType.APPEND -> {
                        if (posts.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    type = KeyType.BEFORE,
                                    id = posts.last().id
                                )
                            )
                        }
                    }
                }

                postDao.insert(posts.map(PostEntity::fromDto))
            }

            return MediatorResult.Success(endOfPaginationReached = isEnd)

        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}