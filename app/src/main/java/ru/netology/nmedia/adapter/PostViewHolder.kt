package ru.netology.nmedia.adapter

import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
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
        publishDate.text = post.published

        root.setOnClickListener { onInteractionListener.onOpen(post) }

        likeIcon.apply {
            isChecked = post.likedByMe
            text = formatCount(post.likes)
            setOnClickListener { onInteractionListener.onLike(post) }
        }

        shareButton.apply {
            text = formatCount(post.shares)
            setOnClickListener { onInteractionListener.onShare(post) }
        }

        viewButton.text = formatCount(post.views)

        menu.setOnClickListener {
            menu.isChecked = true
            val popup = PopupMenu(it.context, it).apply { inflate(R.menu.post_options) }
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.remove -> { onInteractionListener.onRemove(post); true }
                    R.id.edit   -> { onInteractionListener.onEdit(post);   true }
                    else -> false
                }
            }
            popup.setOnDismissListener { menu.isChecked = false }
            popup.show()
        }

        val hasVideo = !post.video.isNullOrBlank()
        videoGroup.isVisible = hasVideo
        if (hasVideo) {
            val url = post.video!!
            videoGroup.setOnClickListener { onInteractionListener.onOpenVideo(url) }
            btnPlay.setOnClickListener    { onInteractionListener.onOpenVideo(url) }
        } else {
            videoGroup.setOnClickListener(null)
            btnPlay.setOnClickListener(null)
        }
    }
}