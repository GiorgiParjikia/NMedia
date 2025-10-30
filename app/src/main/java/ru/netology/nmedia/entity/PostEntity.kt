package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity(tableName = "Post_Entity")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Long
)

fun PostEntity.toDto(): Post = Post(
    id = id,
    author = author,
    content = content,
    published = published,
    likedByMe = likedByMe,
    likes = likes
)

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)

fun Post.toEntity(): PostEntity = PostEntity(
    id = id,
    author = author,
    content = content,
    published = published,
    likedByMe = likedByMe,
    likes = likes
)