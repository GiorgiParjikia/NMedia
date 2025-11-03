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

    fun bind(post: Post) {
        binding.apply {
            authorName.text = post.author
            content.text = post.content
            publishDate.text = post.published.toString()

            // ====== АВАТАР ======
            Glide.with(binding.avatarImage)
                .load("http://10.0.2.2:9999/avatars/${post.authorAvatar ?: "default.jpg"}")
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .timeout(10_000)
                .circleCrop()
                .into(binding.avatarImage)

            // ====== ВЛОЖЕНИЕ (Attachment) ======
            if (post.attachment != null && post.attachment.type == AttachmentType.IMAGE) {
                attachmentImage.visibility = View.VISIBLE

                Glide.with(binding.attachmentImage)
                    .load("http://10.0.2.2:9999/images/${post.attachment.url}")
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_24)
                    .timeout(10_000)
                    .into(binding.attachmentImage)
            } else {
                attachmentImage.visibility = View.GONE
            }

            // ====== Лайки ======
            likeIcon.isChecked = post.likedByMe
            likeIcon.text = formatCount(post.likes)
            likeIcon.setOnClickListener { onInteractionListener.onLike(post) }

            // ====== Шеры ======
            shareButton.setOnClickListener { onInteractionListener.onShare(post) }

            // ====== Меню (удалить / редактировать) ======
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
}