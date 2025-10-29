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
import ru.netology.nmedia.util.StringArg
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    companion object {
        var Bundle.textArgs: String? by StringArg
    }

    private val viewModel: PostViewModel by activityViewModels()

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private val isEditMode: Boolean
        get() = arguments?.textArgs != null

    private var suppressNextDraftSave = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)

        // если редактируем существующий пост — подставить текст в поле
        arguments?.textArgs?.let(binding.edit::setText)

        // Нажали "СОХРАНИТЬ"
        binding.save.setOnClickListener {
            val content = binding.edit.text?.toString()?.trim().orEmpty()
            if (content.isNotBlank()) {
                suppressNextDraftSave = true
                viewModel.changeContent(content)
                viewModel.save()
                hideKeyboard()
            } else {
                // TODO: можно добавить Toast с сообщением "Пост не может быть пустым"
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // загрузка черновика, если поле пустое
        if (!isEditMode && binding.edit.text.isNullOrBlank()) {
            val draft = viewModel.getDraft()
            if (draft.isNotBlank()) {
                binding.edit.setText(draft)
            }
        }

        // ✅ Подписка на SingleLiveEvent (оповещение о создании поста)
        viewModel.postCreated.observe(viewLifecycleOwner) {
            // когда ViewModel завершила сохранение — переходим назад
            viewModel.loadPosts()
            findNavController().navigateUp()
        }

        // перехватываем системную "Назад"
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!isEditMode) {
                        val text = binding.edit.text?.toString()?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            viewModel.saveDraft(text)
                        }
                    }
                    hideKeyboard()
                    findNavController().navigateUp()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()

        if (!isEditMode) {
            if (suppressNextDraftSave) {
                suppressNextDraftSave = false
            } else {
                val text = binding.edit.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    viewModel.saveDraft(text)
                }
            }
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