package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

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

@ExperimentalCoroutinesApi
class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepositoryNetworkImpl(
        AppDb.getInstance(application).postDao()
    )

    private val draftRepo = DraftRepository(application)

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState> get() = _state

    // ------------------------ PHOTO ------------------------
    private val _photo = MutableLiveData<PhotoModel?>(null)
    val photo: LiveData<PhotoModel?> get() = _photo

    fun updatePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }
    // -------------------------------------------------------

    val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> get() = _postCreated

    // FLOW → LiveData
    // FLOW → LiveData
    val data: LiveData<FeedModel> =
        AppAuth.getInstance().data
            .flatMapLatest { token ->
                repository.data
                    .map { posts ->
                        posts.map { post ->
                            post.copy(ownedByMe = post.authorId == token?.id)
                        }
                    }
                    .map(::FeedModel)
            }
            .catch { e ->
                e.printStackTrace()
                _state.postValue(FeedModelState(error = true))
            }
            .asLiveData(Dispatchers.Default)

    private val _newerCount = MutableLiveData(0)
    val newerCount: LiveData<Int> get() = _newerCount

    init {
        loadPosts()

        // отслеживаем новые посты
        data.switchMap { feed ->
            val last = feed.posts.firstOrNull()?.id ?: 0L
            repository.getNewer(last)
                .catch { _state.postValue(FeedModelState(error = true)) }
                .asLiveData()
        }.observeForever { count ->
            count?.let {
                _newerCount.value = (_newerCount.value ?: 0) + it
            }
        }
    }

    fun edit(post: Post) {
        edited.value = post

        // 🔥 если у поста есть вложение — загружаем его в preview
        post.attachment?.let {
            _photo.value = PhotoModel(
                uri = Uri.parse("http://10.0.2.2:9999/media/${it.url}"),
                file = null // файла нет — показываем только Preview по http
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

                val currentPhoto = _photo.value
                val newFile = currentPhoto?.file

                val finalPost = when {
                    // ✔ Новый файл выбран → заменяем вложение
                    newFile != null -> post.copy(attachment = null)

                    // ✔ При редактировании фото не трогали — сохраняем старое
                    post.id != 0L && post.attachment != null -> post

                    // ✔ Новый пост без фото → attachment = null
                    else -> post.copy(attachment = null)
                }

                repository.save(finalPost, newFile)

                _postCreated.postValue(Unit)
                edited.value = empty
                _photo.value = null
            }
        }
    }


    fun like(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _state.postValue(_state.value?.copy(loading = true))
                repository.getAllAsync()
                _state.value = FeedModelState()
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()
}