package ru.netology.nmedia.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArgs
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostAdapter(object : OnInteractionListener {

            override fun onOpen(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_singlePostFragment,
                    Bundle().apply { putLong("postId", post.id) }
                )
            }

            override fun onLike(post: Post) {
                viewModel.like(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, post.content)
                            type = "text/plain"
                        },
                        getString(R.string.chooser_share_post)
                    )
                )
            }

            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply { textArgs = post.content }
                )
            }

            override fun onOpenVideo(url: String) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            url.toUri()
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_app_to_open),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // список
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        // наблюдаем за state: FeedModel из viewModel.data
        viewModel.data.observe(viewLifecycleOwner) { state ->
            val posts = state.posts

            // обновили список
            val isNew = posts.size != adapter.itemCount
            adapter.submitList(posts) {
                if (isNew) {
                    binding.list.smoothScrollToPosition(0)
                }
            }

            // состояния UI
            binding.progress.isVisible = state.loading
            binding.empty.isVisible = state.empty
            binding.errorMerge.root.isVisible = state.error
        }

        // кнопка "повторить"
        binding.errorMerge.retry.setOnClickListener {
            viewModel.loadPosts()
        }

        // FAB "+"
        binding.fab.setOnClickListener {
            viewModel.clearEdit()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }
}