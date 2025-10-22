package ru.netology.nmedia.model

enum class Action {
    LIKE,
    NEW_POST,
    UNKNOWN;

    companion object {
        fun from(raw: String?): Action =
            entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}