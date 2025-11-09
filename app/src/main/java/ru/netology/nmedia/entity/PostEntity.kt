package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post

@Entity(tableName = "Post_Entity")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,                     // локальный ID (генерируется Room)
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int,

    // 🔹 Вложенное поле для картинки/видео
    @Embedded(prefix = "attachment_")
    val attachment: AttachmentEmbeddable? = null,

    // 🔹 Новые поля для задачи №2
    val isLocal: Boolean = false,     // true — пост создан офлайн, ещё не отправлен на сервер
    val localId: Long? = null         // вспомогательный ID, связывающий локальную и серверную версии
) {

    // Преобразуем Entity → DTO
    fun toDto(): Post = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        attachment = attachment?.toDto()
    )

    companion object {
        // Преобразуем DTO → Entity
        fun fromDto(dto: Post, isLocal: Boolean = false, localId: Long? = null): PostEntity =
            PostEntity(
                id = dto.id,
                author = dto.author,
                authorAvatar = dto.authorAvatar,
                content = dto.content,
                published = dto.published,
                likedByMe = dto.likedByMe,
                likes = dto.likes,
                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
                isLocal = isLocal,
                localId = localId
            )
    }
}

// ====== Attachment Embeddable ======
data class AttachmentEmbeddable(
    val url: String,
    val type: String
) {
    fun toDto() = Attachment(
        url = url,
        type = ru.netology.nmedia.dto.AttachmentType.valueOf(type)
    )

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(
                url = it.url,
                type = it.type.name
            )
        }
    }
}
