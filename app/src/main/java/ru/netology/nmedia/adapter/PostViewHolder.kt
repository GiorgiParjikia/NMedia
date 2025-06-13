package ru.netology.nmedia.adapter

import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onLikeListener: (Post) -> Unit,
    private val onShareListener: (Post) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) = with(binding) {
        authorName.text = post.author
        content.text = post.content
        publishDate.text = post.published
        likeCount.text = formatCount(post.likes)
        shareCount.text = formatCount(post.shares)

        likeIcon.setImageResource(
            if (post.likeByMe) R.drawable.favorite_24dp_ea3323
            else R.drawable.favorite_border_24dp_1f1f1f
        )

        likeIcon.setOnClickListener {
            onLikeListener(post)
        }

        shareIcon.setOnClickListener {
            onShareListener(post)
        }
    }
}
