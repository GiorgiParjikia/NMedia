package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post

typealias OnLikeListener = (Post) -> Unit
typealias OnShareListener = (Post) -> Unit
typealias OnRemoveListener = (Post) -> Unit

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onShare(post: Post)
    fun onOpenVideo(url: String) {}
    fun onOpen(post: Post) {}
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener,
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}