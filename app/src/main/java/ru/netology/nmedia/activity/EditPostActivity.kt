package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.netology.nmedia.databinding.ActivityEditPostBinding

class EditPostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POST_ID = "ru.netology.nmedia.extra.POST_ID"
        const val EXTRA_POST_CONTENT = "ru.netology.nmedia.extra.POST_CONTENT"
    }

    private lateinit var binding: ActivityEditPostBinding
    private var postId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getLongExtra(EXTRA_POST_ID, -1L)
        val initial = intent.getStringExtra(EXTRA_POST_CONTENT).orEmpty()
        binding.edit.setText(initial)
        binding.edit.requestFocus()

        binding.btnSave.setOnClickListener {
            val text = binding.edit.text?.toString().orEmpty()
            if (postId > 0L && text.isNotBlank()) {
                setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        putExtra(EXTRA_POST_ID, postId)
                        putExtra(Intent.EXTRA_TEXT, text) // Важно: именно EXTRA_TEXT
                    }
                )
            } else {
                setResult(Activity.RESULT_CANCELED)
            }
            finish()
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}