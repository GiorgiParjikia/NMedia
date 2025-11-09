package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    author = "Giorgi",
    authorAvatar = null,
    published = 0,
    content = "",
    likedByMe = false,
    likes = 0
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepositoryNetworkImpl(
        AppDb.getInstance(application).postDao()
    )

    private val draftRepo = DraftRepository(application)

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState> get() = _state

    // Основной фид (Flow → LiveData)
    val data: LiveData<FeedModel> = repository.data
        .map { posts -> FeedModel(posts, posts.isEmpty()) }
        .catch {
            it.printStackTrace()
            _state.postValue(FeedModelState(error = true))
        }
        .asLiveData(Dispatchers.Default)

    // Счётчик новых постов
    private val _newerCount = MutableLiveData(0)
    val newerCount: LiveData<Int> get() = _newerCount

    init {
        loadPosts()

        // Реактивное отслеживание новых постов
        data.switchMap { feed ->
            val lastSeenId = feed.posts.firstOrNull()?.id ?: 0L

            repository.getNewer(lastSeenId)
                .catch {
                    _state.postValue(FeedModelState(error = true))
                }
                .asLiveData()
        }.observeForever { count ->
            if (count != null && count > 0) {
                // главное исправление — накапливаем число
                val old = _newerCount.value ?: 0
                _newerCount.postValue(old + count)
            }
        }
    }

    // Нажатие "Показать новые"
    fun showNewPosts() {
        viewModelScope.launch {
            try {
                repository.getAllAsync()
                _newerCount.postValue(0)
            } catch (_: Exception) {
                _state.postValue(FeedModelState(error = true))
            }
        }
    }

    val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> get() = _postCreated

    // Загрузка постов
    fun loadPosts() {
        viewModelScope.launch {
            _state.postValue(_state.value?.copy(loading = true))
            try {
                repository.getAllAsync()
                _state.value = FeedModelState()
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    // Лайк
    fun like(id: Long) {
        viewModelScope.launch {
            try {
                repository.likeById(id)
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    // Удаление
    fun removeById(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeById(id)
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    // Сохранение
    fun save() {
        viewModelScope.launch {
            edited.value?.let {
                repository.save(it)
                _postCreated.postValue(Unit)
                edited.value = empty
            }
        }
    }

    // Изменение контента
    fun changeContent(content: String) {
        val text = content.trim()
        val current = edited.value ?: empty
        if (text == current.content) return
        edited.value = current.copy(content = text)
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun clearEdit() {
        edited.value = empty
    }

    // Черновики
    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    // Pull-to-refresh
    fun refresh() {
        viewModelScope.launch {
            _state.postValue(_state.value?.copy(refreshing = true))
            try {
                repository.retryUnsyncedPosts()
                repository.getAllAsync()
                _state.value = FeedModelState()
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }

    // Повторная отправка офлайн-постов
    fun retryUnsyncedPosts() {
        viewModelScope.launch {
            _state.postValue(_state.value?.copy(loading = true))
            try {
                repository.retryUnsyncedPosts()
                _state.value = FeedModelState()
            } catch (_: Exception) {
                _state.value = FeedModelState(error = true)
            }
        }
    }
}