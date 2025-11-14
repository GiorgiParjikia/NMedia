package ru.netology.nmedia.adapter

import ru.netology.nmedia.dto.Post

interface OnInteractionListener {

    fun onLike(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onShare(post: Post)

    fun onOpen(post: Post) {}
    fun onOpenVideo(url: String) {}

    fun onImage(url: String) {}
}
