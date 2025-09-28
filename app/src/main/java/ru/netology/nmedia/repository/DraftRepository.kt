package ru.netology.nmedia.repository

import android.content.Context
import androidx.core.content.edit

class DraftRepository(context: Context) {
    private val prefs = context.getSharedPreferences("draft_prefs", Context.MODE_PRIVATE)

    fun save(text: String) = prefs.edit { putString(KEY, text) }
    fun get(): String = prefs.getString(KEY, "") ?: ""
    fun clear() = prefs.edit { remove(KEY) }

    private companion object {
        const val KEY = "new_post_draft"
    }
}