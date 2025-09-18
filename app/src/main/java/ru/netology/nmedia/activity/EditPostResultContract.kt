// ru/netology/nmedia/activity/EditPostResultContract.kt
package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class EditPostResultContract : ActivityResultContract<Pair<Long, String>, Pair<Long, String>?>() {

    override fun createIntent(context: Context, input: Pair<Long, String>): Intent =
        Intent(context, EditPostActivity::class.java).apply {
            putExtra(EditPostActivity.EXTRA_POST_ID, input.first)
            putExtra(EditPostActivity.EXTRA_POST_CONTENT, input.second)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Long, String>? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        val id = intent.getLongExtra(EditPostActivity.EXTRA_POST_ID, -1L)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        if (id <= 0L || text.isBlank()) return null
        return id to text
    }
}