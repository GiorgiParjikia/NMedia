package ru.netology.nmedia

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val post = Post(
            id = 1,
            author = "Нетология. Университет интернет-профессий будущего",
            published = "21 мая в 18:36",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb"
        )

        with(binding) {
            authorName.text = post.author
            content.text = post.content
            publishDate.text = post.published
            likeCount.text = formatCount(post.likes)
            shareCount.text = formatCount(post.shares)

            likeIcon.setImageResource(
                if (post.likeByMe) R.drawable.favorite_24dp_ea3323
                else R.drawable.favorite_border_24dp_1f1f1f
            )

            likeIcon.setOnClickListener {
                post.likeByMe = !post.likeByMe
                post.likes += if (post.likeByMe) 1 else -1

                likeIcon.setImageResource(
                    if (post.likeByMe) R.drawable.favorite_24dp_ea3323
                    else R.drawable.favorite_border_24dp_1f1f1f
                )
                likeCount.text = formatCount(post.likes)
            }

            shareIcon.setOnClickListener {
                post.shares += 1
                shareCount.text = formatCount(post.shares)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}