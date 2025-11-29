package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val empty = Post(
    id = 0,
    author = "Giorgi",
    authorId = 0,
    authorAvatar = null,
    published = 0,
    content = "",
    likedByMe = false,
    likes = 0,
    attachment = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val draftRepo: DraftRepository,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState> = _state

    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> = _photo

    val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    val data: Flow<PagingData<FeedItem>> =
        appAuth.data
            .flatMapLatest { token ->
                repository.data
                    .map { pagingData ->
                        pagingData
                            .map { item ->
                                if (item is Post) {
                                    item.copy(ownedByMe = item.authorId == token?.id)
                                } else item
                            }
                            .insertSeparators { before, after ->
                                if (before is Post && before.id % 5L == 0L) {
                                    Ad(
                                        id = -before.id,
                                        image = "figma.jpg"
                                    )
                                } else null
                            }
                    }
            }
            .flowOn(Dispatchers.Default)

    init {
        loadPosts()
    }

    fun updatePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    fun edit(post: Post) {
        edited.value = post
        post.attachment?.let {
            _photo.value = PhotoModel(
                Uri.parse("http://10.0.2.2:9999/media/${it.url}"),
                null
            )
        } ?: run {
            _photo.value = null
        }
    }

    fun clearEdit() {
        edited.value = empty
        _photo.value = null
    }

    fun changeContent(content: String) {
        val trimmed = content.trim()
        val post = edited.value ?: empty
        if (trimmed != post.content) {
            edited.value = post.copy(content = trimmed)
        }
    }

    fun save() {
        viewModelScope.launch {
            edited.value?.let { post ->
                val newFile = _photo.value?.file
                val finalPost = when {
                    newFile != null -> post.copy(attachment = null)
                    post.id != 0L && post.attachment != null -> post
                    else -> post.copy(attachment = null)
                }

                repository.save(finalPost, newFile)

                _postCreated.postValue(Unit)
                edited.value = empty
                _photo.value = null
            }
        }
    }

    fun like(id: Long) = viewModelScope.launch {
        try {
            repository.likeById(id)
        } catch (_: Exception) {
            _state.value = FeedModelState(error = true)
        }
    }

    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (_: Exception) {
            _state.value = FeedModelState(error = true)
        }
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _state.postValue(FeedModelState(loading = true))
            _state.value = FeedModelState()
        } catch (_: Exception) {
            _state.value = FeedModelState(error = true)
        }
    }

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()
}