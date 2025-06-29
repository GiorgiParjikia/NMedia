package ru.netology.nmedia.adapter

import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) = with(binding) {
        authorName.text = post.author
        content.text = post.content
        publishDate.text = post.published

        likeIcon.apply {
            isChecked = post.likeByMe
            text = formatCount(post.likes)
            setOnClickListener {
                onInteractionListener.onLike(post)
            }
        }

        shareButton.apply {
            text = formatCount(post.shares)
            setOnClickListener {
                onInteractionListener.onShare(post)
            }
        }

        viewButton.text = formatCount(post.views)

        menu.setOnClickListener {
            menu.isChecked = true

            val popup = PopupMenu(it.context, it)
            popup.inflate(R.menu.post_options)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.remove -> {
                        onInteractionListener.onRemove(post)
                        true
                    }
                    R.id.edit -> {
                        onInteractionListener.onEdit(post)
                        true
                    }
                    else -> false
                }
            }

            popup.setOnDismissListener {
                menu.isChecked = false
            }

            popup.show()
        }
    }
}