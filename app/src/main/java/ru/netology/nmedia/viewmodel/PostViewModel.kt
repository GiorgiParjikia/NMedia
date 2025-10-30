package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.util.SingleLiveEvent
import kotlin.concurrent.thread

// пост-заглушка для редактирования/создания
private val empty = Post(
    id = 0,
    author = "Me",
    published = "now",
    content = "",
    likedByMe = false,
    likes = 0,
    shares = 0,
    views = 0,
    video = null,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepositoryNetworkImpl()
    private val draftRepo = DraftRepository(application)

    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> get() = _data

    val edited = MutableLiveData(empty)

    // 🔹 SingleLiveEvent для одноразовых событий (например, создание поста)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            _data.postValue(
                FeedModel(
                    posts = _data.value?.posts ?: emptyList(),
                    loading = true,
                    error = false,
                    empty = false,
                )
            )

            try {
                val posts = repository.getAll()
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        loading = false,
                        error = false,
                        empty = posts.isEmpty(),
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(
                    FeedModel(
                        posts = _data.value?.posts ?: emptyList(),
                        loading = false,
                        error = true,
                        empty = false,
                    )
                )
            }
        }
    }

    fun like(id: Long) {
        thread {
            try {
                repository.likeById(id)
                val posts = repository.getAll()
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        loading = false,
                        error = false,
                        empty = posts.isEmpty(),
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(
                    _data.value?.copy(
                        loading = false,
                        error = true,
                    )
                )
            }
        }
    }

    fun removeById(id: Long) {
        thread {
            try {
                repository.removeById(id)
                val posts = repository.getAll()
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        loading = false,
                        error = false,
                        empty = posts.isEmpty(),
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(
                    _data.value?.copy(
                        loading = false,
                        error = true,
                    )
                )
            }
        }
    }

    // 🔹 Обновлённый метод save() с вызовом SingleLiveEvent
    fun save() {
        val postToSave = edited.value ?: return

        thread {
            try {
                repository.save(postToSave)
                edited.postValue(empty)
                clearDraft()

                _postCreated.postValue(Unit) // 👈 уведомляем фрагмент один раз

                val posts = repository.getAll()
                _data.postValue(
                    FeedModel(
                        posts = posts,
                        loading = false,
                        error = false,
                        empty = posts.isEmpty(),
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(
                    _data.value?.copy(
                        loading = false,
                        error = true,
                    )
                )
            }
        }
    }

    fun changeContent(content: String) {
        val text = content.trim()
        val current = edited.value ?: return
        if (text == current.content) return
        edited.value = current.copy(content = text)
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun clearEdit() {
        edited.value = empty
    }

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    fun retry() {
        loadPosts()
    }
}