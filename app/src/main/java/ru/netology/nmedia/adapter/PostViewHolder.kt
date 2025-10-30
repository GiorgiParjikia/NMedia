package ru.netology.nmedia.adapter

import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.formatCount

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) = with(binding) {
        authorName.text = post.author
        content.text = post.content
        publishDate.text = post.published.toString() // 👈 published — теперь Long

        root.setOnClickListener { onInteractionListener.onOpen(post) }

        likeIcon.apply {
            isChecked = post.likedByMe
            text = formatCount(post.likes)
            setOnClickListener { onInteractionListener.onLike(post) }
        }

        // Убрали shares и views, оставляем только лайки и меню
        shareButton.setOnClickListener { onInteractionListener.onShare(post) }

        menu.setOnClickListener {
            val popup = PopupMenu(it.context, it).apply {
                inflate(R.menu.post_options)
                setOnMenuItemClickListener { item ->
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
            }
            popup.show()
        }
    }
}