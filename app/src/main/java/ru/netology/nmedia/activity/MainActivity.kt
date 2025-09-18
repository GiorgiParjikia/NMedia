package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: PostViewModel by viewModels()

    private val newPostLauncher = registerForActivityResult(NewPostResultContract()) { content ->
        content ?: return@registerForActivityResult
        viewModel.changeContent(content)
        viewModel.save()
    }

    private val editLauncher = registerForActivityResult(EditPostResultContract()) { result ->
        result ?: return@registerForActivityResult
        val (_, text) = result
        viewModel.changeContent(text)
        viewModel.save()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val adapter = PostAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.like(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                viewModel.share(post.id)

                val send = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(send, getString(R.string.chooser_share_post)))
            }

            override fun onEdit(post: Post) {
                viewModel.edit(post)
                editLauncher.launch(post.id to post.content)
            }
        })

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        viewModel.data.observe(this) { posts ->
            val isNew = posts.size != adapter.itemCount
            adapter.submitList(posts) {
                if (isNew) binding.list.smoothScrollToPosition(0)
            }
        }

        binding.fab.setOnClickListener {
            newPostLauncher.launch(Unit)
        }
    }
}