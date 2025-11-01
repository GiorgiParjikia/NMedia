package ru.netology.nmedia.util

import kotlin.math.floor

fun formatCount(count: Int): String {
    return when {
        count < 1_000 -> count.toString()
        count < 10_000 -> {
            val shortValue = floor(count / 100.0) / 10
            "${shortValue}K"
        }
        count < 1_000_000 -> "${count / 1_000}K"
        else -> {
            val shortValue = floor(count / 100_000.0) / 10
            "${shortValue}M"
        }
    }
}
