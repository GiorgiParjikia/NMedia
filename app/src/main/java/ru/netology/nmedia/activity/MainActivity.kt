package ru.netology.nmedia.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.viewmodel.PostViewModel
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.formatCount

class MainActivity : AppCompatActivity() {

    private val viewModel: PostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        viewModel.data.observe(this) { post ->
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
                    viewModel.like()
                }

                shareIcon.setOnClickListener {
                    viewModel.share()
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}