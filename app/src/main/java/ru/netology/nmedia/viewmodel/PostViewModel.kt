package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositorySQLiteImpl
import ru.netology.nmedia.repository.DraftRepository

private val empty = Post(
    id = 0,
    author = "",
    published = "",
    content = "",
    likeByMe = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepositorySQLiteImpl(
        AppDb.getInstance(application).postDao
    )

    private val draftRepo = DraftRepository(application)

    val data: LiveData<List<Post>> = repository.get()

    val edited = MutableLiveData(empty)

    fun saveDraft(text: String) = draftRepo.save(text)
    fun getDraft(): String = draftRepo.get()
    fun clearDraft() = draftRepo.clear()

    fun like(id: Long) = repository.like(id)
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

    fun edit(post: Post) { edited.value = post }
    fun clearEdit() { edited.value = empty }
}