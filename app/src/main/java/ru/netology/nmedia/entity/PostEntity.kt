package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity(tableName = "Post_Entity")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val published: String,
    val content: String,
    val likes: Long,
    val likeByMe: Boolean,
    val shares: Long,
    val views: Long,
    val video: String?
)

fun PostEntity.toDto(): Post = Post(
    id = id,
    author = author,
    published = published,
    content = content,
    likes = likes,
    likeByMe = likeByMe,
    shares = shares,
    views = views,
    video = video
)

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)

fun Post.toEntity(): PostEntity = PostEntity(
    id = id,
    author = author,
    published = published,
    content = content,
    likes = likes,
    likeByMe = likeByMe,
    shares = shares,
    views = views,
    video = video
)
