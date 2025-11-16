package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post

@Entity(tableName = "Post_Entity")
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,

    val author: String,
    val authorId: Long,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int,

    @Embedded(prefix = "attachment_")
    val attachment: AttachmentEmbeddable? = null,

    val isLocal: Boolean = false,
    val localId: Long? = null,
    val isHidden: Boolean = false,
) {

    fun toDto(): Post = Post(
        id = id,
        author = author,
        authorId = authorId,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        attachment = attachment?.toDto(),
    )

    companion object {
        fun fromDto(
            dto: Post,
            isLocal: Boolean = false,
            localId: Long? = null,
            isHidden: Boolean = false,
        ): PostEntity =
            PostEntity(
                id = dto.id,
                author = dto.author,
                authorId = dto.authorId,
                authorAvatar = dto.authorAvatar,
                content = dto.content,
                published = dto.published,
                likedByMe = dto.likedByMe,
                likes = dto.likes,
                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
                isLocal = isLocal,
                localId = localId,
                isHidden = isHidden,
            )
    }
}

data class AttachmentEmbeddable(
    val url: String,
    val type: String,
) {
    fun toDto() = Attachment(
        url = url,
        type = AttachmentType.valueOf(type),
    )

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(
                url = it.url,
                type = it.type.name,
            )
        }
    }
}