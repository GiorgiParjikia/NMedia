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
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
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
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        // Адаптер
        val adapter = PostAdapter(object : OnInteractionListener {

            override fun onOpen(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_singlePostFragment,
                    Bundle().apply { putLong("postId", post.id) }
                )
            }

            override fun onLike(post: Post) = viewModel.like(post.id)

            override fun onRemove(post: Post) = viewModel.removeById(post.id)

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
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }

            override fun onOpenVideo(url: String) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.no_app_to_open),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // Настройка списка
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        // Swipe Refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // Главный фид — корректный submitList + автоскролл
        viewModel.data.observe(viewLifecycleOwner) { model ->
            adapter.submitList(model.posts) {
                // Скроллим вверх только если пользователь нажал "показать новые"
                if (viewModel.newerCount.value == 0) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
            binding.empty.isVisible = model.empty
        }

        // Плашка новых постов
        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
            binding.newPostsCard.isVisible = count > 0
            binding.newPostsText.text =
                getString(R.string.show_new_posts, count)
        }

        // Нажатие на плашку — скрыть и загрузить
        binding.newPostsCard.setOnClickListener {
            binding.newPostsCard.isVisible = false
            viewModel.showNewPosts()
        }

        // Ошибки и загрузка
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing

            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { viewModel.refresh() }
                    .show()
            }
        }

        // FAB
        binding.fab.setOnClickListener {
            viewModel.clearEdit()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }
}