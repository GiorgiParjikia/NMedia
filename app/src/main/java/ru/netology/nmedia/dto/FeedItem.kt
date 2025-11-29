package ru.netology.nmedia.dto

sealed interface FeedItem {
    val id: Long
}

// 🔹 Разделитель "Сегодня"
data class TodaySeparator(
    override val id: Long = Long.MIN_VALUE + 1,
    val title: String = "Сегодня"
) : FeedItem

// 🔹 Разделитель "Вчера"
data class YesterdaySeparator(
    override val id: Long = Long.MIN_VALUE + 2,
    val title: String = "Вчера"
) : FeedItem

// 🔹 Разделитель "На прошлой неделе"
data class LastWeekSeparator(
    override val id: Long = Long.MIN_VALUE + 3,
    val title: String = "На прошлой неделе"
) : FeedItem
