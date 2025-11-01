package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity(tableName = "Post_Entity")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorAvatar: String?, // 👈 добавляем сюда
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int
) {
    fun toDto(): Post = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar, // 👈 добавляем и сюда
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes
    )

    companion object {
        fun fromDto(dto: Post): PostEntity = PostEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar, // 👈 и сюда
            content = dto.content,
            published = dto.published,
            likedByMe = dto.likedByMe,
            likes = dto.likes
        )
    }
}