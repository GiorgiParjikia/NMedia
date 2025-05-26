package ru.netology.nmedia

fun formatCount(count: Int): String {
    return when {
        count < 1_000 -> count.toString()
        count < 10_000 -> String.format("%.1fK", count / 1_000.0)
        count < 1_000_000 -> "${count / 1_000}K"
        else -> String.format("%.1fM", count / 1_000_000.0)
    }
}
