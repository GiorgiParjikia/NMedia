package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.formatCount
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class SinglePostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private var _binding: FragmentSinglePostBinding? = null
    private val binding get() = _binding!!

    private var postId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = requireArguments().getLong("postId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSinglePostBinding.inflate(inflater, container, false)

        binding.post.root.setOnClickListener(null)

        viewModel.data.observe(viewLifecycleOwner) { feed ->
            val post = feed.posts.firstOrNull { it.id == postId } ?: return@observe
            bindPost(post)
        }

        return binding.root
    }

    private fun bindPost(post: Post) = with(binding.post) {
        authorName.text = post.author
        content.text = post.content
        publishDate.text = AndroidUtils.formatDate(post.published)

        likeIcon.isChecked = post.likedByMe
        likeIcon.text = formatCount(post.likes)

        likeIcon.setOnClickListener {
            viewModel.like(post.id)
        }

        shareButton.setOnClickListener {
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

        menu.setOnClickListener { anchor ->
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.post_options)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {

                        R.id.remove -> {
                            viewModel.removeById(post.id)
                            findNavController().popBackStack()
                            true
                        }

                        R.id.edit -> {
                            viewModel.edit(post)
                            findNavController().navigate(
                                R.id.action_singlePostFragment_to_newPostFragment
                            )
                            true
                        }

                        else -> false
                    }
                }
            }.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}