package ru.netology.nmedia.entity

import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType

data class AttachmentEmbeddable(
    val url: String,
    val description: String?,
    val type: AttachmentType
) {
    fun toDto() = Attachment(url, description, type)

    companion object {
        fun fromDto(dto: Attachment?) =
            dto?.let { AttachmentEmbeddable(it.url, it.description, it.type) }
    }
}