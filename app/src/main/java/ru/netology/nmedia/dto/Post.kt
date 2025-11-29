package ru.netology.nmedia.dto

sealed interface FeedItem {
    val id: Long
}

data class Post(
    override val id: Long,
    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val ownedByMe: Boolean = false,
    val attachment: Attachment? = null,
) : FeedItem


data class Ad (
    override val id: Long,
    val image: String,
) : FeedItem