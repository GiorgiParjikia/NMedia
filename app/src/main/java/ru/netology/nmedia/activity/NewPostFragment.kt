package ru.netology.nmedia.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var suppressNextDraftSave = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)

        // ✅ Наблюдаем за редактируемым постом
        viewModel.edited.observe(viewLifecycleOwner) { post ->
            binding.edit.setText(post.content)
        }

        // ✅ Кнопка "СОХРАНИТЬ"
        binding.save.setOnClickListener {
            val content = binding.edit.text?.toString()?.trim().orEmpty()
            if (content.isNotBlank()) {
                suppressNextDraftSave = true
                viewModel.changeContent(content)
                viewModel.save()
                hideKeyboard()
            } else {
                // Можно добавить Toast: "Пост не может быть пустым"
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Если редактирования нет, подгружаем черновик
        val draft = viewModel.getDraft()
        if (draft.isNotBlank() && binding.edit.text.isNullOrBlank()) {
            binding.edit.setText(draft)
        }

        // Переход назад после сохранения
        viewModel.postCreated.observe(viewLifecycleOwner) {
            viewModel.loadPosts()
            findNavController().navigateUp()
        }

        // Обработка кнопки "Назад"
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val text = binding.edit.text?.toString()?.trim().orEmpty()
                    if (text.isNotEmpty()) viewModel.saveDraft(text)
                    hideKeyboard()
                    findNavController().navigateUp()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()

        if (!suppressNextDraftSave) {
            val text = binding.edit.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) viewModel.saveDraft(text)
        } else {
            suppressNextDraftSave = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideKeyboard() {
        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}