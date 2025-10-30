package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: Long,       // 👈 именно Long, не String!
    val likedByMe: Boolean = false,
    val likes: Long = 0
)