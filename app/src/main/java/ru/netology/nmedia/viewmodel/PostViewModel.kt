package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl
import kotlin.concurrent.thread

private val empty = Post(
    id = 0,
    author = "Me",
    published = "now",
    content = "",
    likeByMe = false,
    likes = 0,
    shares = 0,
    views = 0,
    video = null,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    // сетевой репозиторий
    private val repository = PostRepositoryNetworkImpl()

    // локальные черновики
    private val draftRepo = DraftRepository(application)

    // ============== состояние экрана ленты ==============
    // FeedModel = (posts + loading + error + empty)
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> get() = _data

    // текущий редактируемый пост (для NewPostFragment / редактирования существующего)
    val edited = MutableLiveData(empty)

    init {
        // сразу грузим посты при старте VM
        loadPosts()
    }

    // Подгрузка ленты с сервера
    fun loadPosts() {
        thread {
            // показываем прогресс
            _data.postValue(
                FeedModel(
                    posts = _data.value?.posts ?: emptyList(),
                    loading = true,
                    error = false,
                    empty = false
                )
            )

            // пробуем получить список постов
            val newState = try {
                val posts = repository.getAllBlocking()
                FeedModel(
                    posts = posts,
                    loading = false,
                    error = false,
                    empty = posts.isEmpty()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                FeedModel(
                    posts = _data.value?.posts ?: emptyList(),
                    loading = false,
                    error = true,
                    empty = false
                )
            }

            _data.postValue(newState)
        }
    }

    // лайк / дизлайк
    fun like(id: Long) {
        thread {
            try {
                repository.like(id)
            } finally {
                // после лайка просто перезагрузим, чтобы числа и флажок обновились
                loadPosts()
            }
        }
    }

    // удалить пост
    fun removeById(id: Long) {
        thread {
            try {
                repository.removeById(id)
            } finally {
                loadPosts()
            }
        }
    }

    // сохранить (новый пост или отредактированный)
    fun save() {
        val postToSave = edited.value ?: return

        thread {
            try {
                repository.save(postToSave)
            } finally {
                // сбрасываем состояние "редактируемого"
                edited.postValue(empty)
                // очищаем черновик после успешного сохранения
                clearDraft()
                // перезагружаем ленту
                loadPosts()
            }
        }
    }

    // вызвать при наборе текста
    fun changeContent(content: String) {
        val text = content.trim()
        val current = edited.value ?: return
        if (text == current.content) return

        edited.value = current.copy(content = text)
    }

    // начать редактировать существующий пост
    fun edit(post: Post) {
        edited.value = post
    }

    // сбросить состояние редактируемого поста (когда жмём + или уходим без сохранения)
    fun clearEdit() {
        edited.value = empty
    }

    // ---------------- Черновики ----------------

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    // retry из экрана ошибки
    fun retry() {
        loadPosts()
    }
}