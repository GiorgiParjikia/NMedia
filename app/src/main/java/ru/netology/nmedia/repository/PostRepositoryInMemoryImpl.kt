package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl : PostRepository {

    private var index: Long = 1L
    private var posts = listOf(
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "21 мая в 18:36",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "22 мая в 12:22",
            content = "Как отличить программиста-интроверта от экстраверта?\n" +
                    "Интроверт смотрит на свои ботинки, когда с тобой разговаривает.\n" +
                    "Экстраверт — на твои.",
            video = "https://youtu.be/xT8oP0wy-A0?si=Y2_Lv2RA4PXPwxIa",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "23 мая в 10:05",
            content = "UX — это не просто красиво. Это когда пользователь не замечает интерфейс, потому что всё работает интуитивно. Помните: хорошо — когда незаметно.",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "24 мая в 15:42",
            content = "Как понять, что ты разработчик? Ты не доверяешь себе, пока не увидишь `green` в тестах.",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "25 мая в 09:13",
            content = "Маркетинг — это не уговорить купить. Это помочь понять, почему человеку это действительно нужно. Учимся говорить на языке аудитории.",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "26 мая в 14:27",
            content = "Раньше знание Excel открывало двери. Сейчас — SQL, Python и аналитика. Мир меняется, и мы учим быть нужными.",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "27 мая в 17:00",
            content = "«Всё уже придумано до нас» — плохая отмазка, чтобы не начинать. Самые успешные проекты — это часто простые идеи, реализованные вовремя.",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "28 мая в 11:44",
            content = "Совет джуну: не бойся просить помощи. Сеньоры не кусаются. (Пока не видят `var a = 1` в проде 😅)",
            likeByMe = false
        ),
        Post(
            id = index++,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "29 мая в 13:37",
            content = "Знания не устаревают — устаревают подходы. Обучение всю жизнь — не модный тренд, а жизненная необходимость.",
            likeByMe = false
        )
    )

    private val data = MutableLiveData(posts)

    override fun get(): LiveData<List<Post>> = data

    override fun like(id: Long) {
        posts = posts.map { post ->
            if (post.id == id) {
                post.copy(
                    likeByMe = !post.likeByMe,
                    likes = if (post.likeByMe) post.likes - 1 else post.likes + 1
                )
            } else {
                post
            }
        }
        data.value = posts
    }

    override fun share(id: Long) {
        posts = posts.map { post ->
            if (post.id == id) {
                post.copy(shares = post.shares + 1)
            } else {
                post
            }
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            posts = listOf(
                post.copy(
                    id = index++,
                    author = "Me",
                    published = "now"

                )
            ) + posts
            data.value = posts
        } else {
            posts = posts.map {
                if (post.id == it.id) {
                    it.copy(content = post.content)
                } else it
            }
        }
        data.value = posts
    }
}