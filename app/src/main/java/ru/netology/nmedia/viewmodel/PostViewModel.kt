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

// пост-заглушка для редактирования/создания
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

    // наш репозиторий (синхронные вызовы)
    private val repository = PostRepositoryNetworkImpl()

    // храним черновик локально
    private val draftRepo = DraftRepository(application)

    // состояние ленты для UI
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> get() = _data

    // пост, который сейчас редактируем
    val edited = MutableLiveData(empty)

    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            // показать прогресс
            _data.postValue(
                FeedModel(
                    posts = _data.value?.posts ?: emptyList(),
                    loading = true,
                    error = false,
                    empty = false,
                )
            )

            try {
                val posts = repository.getAll() // теперь он синхронно вернёт List<Post>

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
                // после успешного лайка просто перечитаем список
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
                // если ошибка, просто выставим error = true, но не потеряем текущие посты
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

    fun save() {
        val postToSave = edited.value ?: return

        thread {
            try {
                repository.save(postToSave) // вернёт сохранённый пост с id
                edited.postValue(empty)
                clearDraft()

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

    // редактирование текста поста (экран создания/редактирования)
    fun changeContent(content: String) {
        val text = content.trim()
        val current = edited.value ?: return
        if (text == current.content) return
        edited.value = current.copy(content = text)
    }

    // начинаем редактировать существующий пост
    fun edit(post: Post) {
        edited.value = post
    }

    // очистить редактируемый пост (когда жмём "+")
    fun clearEdit() {
        edited.value = empty
    }

    // блок черновика
    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    // retry с кнопки "повторить"
    fun retry() {
        loadPosts()
    }
}