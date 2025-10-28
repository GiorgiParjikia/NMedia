package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.DraftRepository
import ru.netology.nmedia.repository.PostRepositoryNetworkImpl

private val empty = Post(
    id = 0,
    author = "",
    published = "",
    content = "",
    likeByMe = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    // теперь мы работаем через сеть, а не через SQLite
    private val repository = PostRepositoryNetworkImpl()

    private val draftRepo = DraftRepository(application)

    // лента постов для UI
    val data: LiveData<List<Post>> = repository.get()

    // редактируемый пост (у тебя уже было)
    val edited = MutableLiveData(empty)

    init {
        // при старте вьюмодели сразу грузим посты с сервера
        repository.refresh()
    }

    // черновики оставляем как есть, они локальные и не мешают сетевой части
    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    // ЛАЙК теперь идёт в сеть
    fun like(id: Long) = repository.like(id)

    // Остальное пока просто прокидываем в репозиторий,
    // но в PostRepositoryNetworkImpl это заглушки.
    fun share(id: Long) = repository.share(id)
    fun removeById(id: Long) = repository.removeById(id)

    fun changeContent(content: String) {
        val text = content.trim()
        edited.value?.let {
            if (text == it.content) return
            edited.value = it.copy(content = text)
        }
    }

    fun save() {
        edited.value?.let {
            repository.save(it)
        }
        edited.value = empty
        clearDraft()
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun clearEdit() {
        edited.value = empty
    }
}