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
    private val api: PostsApiService,
    private val postDao: PostDao,
    private val keyDao: PostRemoteKeyDao,
    private val db: AppDb,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        return try {
            val response = when (loadType) {

                // Подгрузка сверху (PREPEND)
                LoadType.PREPEND -> {
                    val afterKey = keyDao.getKey(KeyType.AFTER)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)

                    val resp = api.getAfter(afterKey, state.config.pageSize)
                    if (!resp.isSuccessful) throw HttpException(resp)
                    resp
                }

                // Обновление списка (REFRESH)
                LoadType.REFRESH -> {
                    val afterKey = keyDao.getKey(KeyType.AFTER)
                    val resp = if (afterKey == null) {
                        api.getLatest(state.config.pageSize)
                    } else {
                        api.getAfter(afterKey, state.config.pageSize)
                    }
                    if (!resp.isSuccessful) throw HttpException(resp)
                    resp
                }


                LoadType.APPEND -> {
                    val beforeKey = keyDao.getKey(KeyType.BEFORE)
                        ?: return MediatorResult.Success(endOfPaginationReached = true)

                    val resp = api.getBefore(beforeKey, state.config.pageSize)
                    if (!resp.isSuccessful) throw HttpException(resp)
                    resp
                }
            }

            val posts = response.body().orEmpty()
            val endReached = posts.isEmpty()

            db.withTransaction {

                when (loadType) {
                    LoadType.REFRESH -> {
                        if (posts.isNotEmpty()) {
                            val first = posts.first().id
                            val last = posts.last().id
                            val savedAfter = keyDao.getKey(KeyType.AFTER)
                            if (savedAfter == null) {
                                keyDao.insert(PostRemoteKeyEntity(KeyType.AFTER, first))
                                keyDao.insert(PostRemoteKeyEntity(KeyType.BEFORE, last))
                            } else {
                                keyDao.insert(PostRemoteKeyEntity(KeyType.AFTER, first))
                            }
                        }
                    }

                    LoadType.APPEND -> {
                        if (posts.isNotEmpty()) {
                            val last = posts.last().id
                            keyDao.insert(PostRemoteKeyEntity(KeyType.BEFORE, last))
                        }
                    }

                    LoadType.PREPEND -> {
                        if (posts.isNotEmpty()) {
                            val first = posts.first().id
                            keyDao.insert(PostRemoteKeyEntity(KeyType.AFTER, first))
                        }
                    }
                }

                postDao.insert(posts.map(PostEntity::fromDto))
            }

            MediatorResult.Success(endOfPaginationReached = endReached)

        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
