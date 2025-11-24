package ru.netology.nmedia.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

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
                if (!authViewModel.isAuthenticated) {
                    showAuthDialog()
                    return
                }
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
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }

            override fun onOpenVideo(url: String) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), R.string.no_app_to_open, Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onImage(url: String) {
                findNavController().navigate(
                    R.id.fullImageFragment,
                    Bundle().apply { putString("url", url) }
                )
            }
        })

        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            adapter.refresh()
        }
        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest {
                adapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest {
                binding.swipeRefresh.isRefreshing = it.refresh is LoadState.Loading
                        ||it.append is LoadState.Loading
                        ||it.prepend is LoadState.Loading
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { viewModel.loadPosts() }
                    .show()
            }
        }

        binding.fab.setOnClickListener {
            if (!authViewModel.isAuthenticated) {
                showAuthDialog()
                return@setOnClickListener
            }

            viewModel.clearEdit()
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }

    private fun showAuthDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.auth_required_title)
            .setMessage(R.string.auth_required_message)
            .setPositiveButton(R.string.sign_in) { _, _ ->
                findNavController().navigate(R.id.signInFragment)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}