package ru.netology.nmedia.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences("draft_prefs", Context.MODE_PRIVATE)

    fun save(text: String) = prefs.edit { putString(KEY, text) }
    fun get(): String = prefs.getString(KEY, "") ?: ""
    fun clear() = prefs.edit { remove(KEY) }

    private companion object {
        const val KEY = "new_post_draft"
    }
}
