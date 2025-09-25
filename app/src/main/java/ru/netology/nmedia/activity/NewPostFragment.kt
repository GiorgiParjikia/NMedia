package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    companion object { var Bundle.textArgs: String? by StringArg }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(inflater, container, false)

        arguments?.textArgs?.let(binding.edit::setText)

        binding.save.setOnClickListener {
            val content = binding.edit.text?.toString()?.trim().orEmpty()
            if (content.isNotBlank()) {
                viewModel.changeContent(content)
                viewModel.save()
            }
            findNavController().navigateUp()
        }

        return binding.root
    }
}
