package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import ru.netology.nmedia.util.SingleLiveEvent
import kotlin.concurrent.thread

// Пост-заглушка
// Пост-заглушка
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

    private val repository = PostRepositoryNetworkImpl()
    private val draftRepo = DraftRepository(application)

    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> get() = _data

    val edited = MutableLiveData(empty)

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> get() = _postCreated

    init {
        loadPosts()
    }

/*
fun loadPosts() {
        thread {
            _data.postValue(_data.value?.copy(loading = true))
            try {
                val posts = repository.getAll()
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            } catch (e: Exception) {
                e.printStackTrace()
                _data.postValue(_data.value?.copy(error = true, loading = false))
            }
        }
    }
 */

    fun loadPosts() {
        _data.postValue(_data.value?.copy(loading = true))
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.value = FeedModel(posts = posts, empty = posts.isEmpty())
            }

            override fun onError(e: Throwable) {
                _data.value = FeedModel(error = true, loading = false)
            }
        })
    }

    fun like(id: Long) = thread {
        try {
            val likedByMe = _data.value?.posts?.find { it.id == id }?.likedByMe ?: return@thread
            val updatedPost = repository.likeById(id, likedByMe)

            _data.postValue(
                _data.value?.copy(
                    posts = _data.value?.posts.orEmpty().map {
                        if (it.id == id) updatedPost else it
                    }
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _data.postValue(_data.value?.copy(error = true))
        }
    }

    fun removeById(id: Long) = thread {
        try {
            repository.removeById(id)
            loadPosts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save() {
        val postToSave = edited.value ?: return
        thread {
            try {
                repository.save(postToSave)
                edited.postValue(empty)
                clearDraft()
                _postCreated.postValue(Unit)
                loadPosts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changeContent(content: String) {
        val text = content.trim()
        val current = edited.value ?: empty
        if (text == current.content) return

        // 👇 сохраняем старый id (если редактируем)
        edited.value = current.copy(content = text)
    }

    fun edit(post: Post) {
        edited.value = post // сохраняем весь пост
    }

    fun clearEdit() {
        edited.value = empty
    }

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()
}