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

    // 🔹 Загрузка постов
    fun loadPosts() {
        _data.postValue(_data.value?.copy(loading = true))
        repository.getAllAsync(object : PostRepository.GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Throwable) {
                _data.postValue(_data.value?.copy(error = true, loading = false))
            }
        })
    }

    // 🔹 Лайк / дизлайк
    fun like(id: Long) {
        val likedByMe = _data.value?.posts?.find { it.id == id }?.likedByMe ?: return

        repository.likeByIdAsync(id, likedByMe, object : PostRepositoryNetworkImpl.NetworkCallback<Post> {
            override fun onSuccess(updatedPost: Post) {
                _data.postValue(
                    _data.value?.copy(
                        posts = _data.value?.posts.orEmpty().map {
                            if (it.id == id) updatedPost else it
                        }
                    )
                )
            }

            override fun onError(e: Throwable) {
                _data.postValue(_data.value?.copy(error = true))
            }
        })
    }

    // 🔹 Удаление поста
    fun removeById(id: Long) {
        repository.removeByIdAsync(id, object : PostRepositoryNetworkImpl.NetworkCallback<Unit> {
            override fun onSuccess(result: Unit) {
                loadPosts()
            }

            override fun onError(e: Throwable) {
                _data.postValue(_data.value?.copy(error = true))
            }
        })
    }

    // 🔹 Сохранение поста
    fun save() {
        val postToSave = edited.value ?: return

        repository.saveAsync(postToSave, object : PostRepositoryNetworkImpl.NetworkCallback<Post> {
            override fun onSuccess(result: Post) {
                edited.postValue(empty)
                clearDraft()
                _postCreated.postValue(Unit)
                loadPosts()
            }

            override fun onError(e: Throwable) {
                _data.postValue(_data.value?.copy(error = true))
            }
        })
    }

    // 🔹 Изменение текста поста
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

    // 🔹 Черновики
    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()
}