package ru.netology.nmedia.adapter

import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.formatCount

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) = with(binding) {

        authorName.text = post.author
        content.text = post.content
        publishDate.text = post.published.toString()

        // ====== АВАТАР ======
        Glide.with(avatarImage)
            .load("http://10.0.2.2:9999/avatars/${post.authorAvatar}")
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .timeout(10_000)
            .circleCrop()
            .into(avatarImage)

        // ====== ВЛОЖЕНИЕ ======
        if (post.attachment != null && post.attachment.type == AttachmentType.IMAGE) {

            attachmentImage.visibility = View.VISIBLE

            val imageUrl = "http://10.0.2.2:9999/media/${post.attachment.url}"

            Glide.with(attachmentImage)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_error_24)
                .timeout(10_000)
                .into(attachmentImage)

            // 👉 Передаём именно URL
            attachmentImage.setOnClickListener {
                onInteractionListener.onImage(imageUrl)
            }

        } else {
            attachmentImage.visibility = View.GONE
        }

        likeIcon.isChecked = post.likedByMe
        likeIcon.text = formatCount(post.likes)
        likeIcon.setOnClickListener { onInteractionListener.onLike(post) }

        shareButton.setOnClickListener { onInteractionListener.onShare(post) }

        menu.setOnClickListener {
            PopupMenu(it.context, it).apply {
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
            }.show()
        }
    }
}