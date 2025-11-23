package ru.netology.nmedia.activity

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!

    private var suppressNextDraftSave = false

    private val photoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.image_pick_error),
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            val uri = result.data?.data ?: return@registerForActivityResult
            viewModel.updatePhoto(uri, uri.toFile())
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)

        if (viewModel.edited.value?.id == 0L) {
            viewModel.removePhoto()
        }

        setupMenu()
        setupObservers()
        setupButtons()

        return binding.root
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                    inflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {
                        R.id.save -> {
                            val content = binding.edit.text.toString().trim()
                            val photo = viewModel.photo.value

                            if (content.isBlank() && photo == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Нельзя опубликовать пустой пост",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return true
                            }

                            suppressNextDraftSave = true
                            viewModel.changeContent(content)
                            viewModel.save()
                            hideKeyboard()
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner
        )
    }

    private fun setupObservers() {
        viewModel.photo.observe(viewLifecycleOwner) { photo ->
            if (photo == null) {
                binding.previewContainer.isGone = true
                binding.preview.setImageDrawable(null)
                return@observe
            }

            binding.previewContainer.isVisible = true

            Glide.with(binding.preview)
                .load(photo.uri)
                .into(binding.preview)
        }

        viewModel.edited.observe(viewLifecycleOwner) { post ->
            binding.edit.setText(post.content)
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            viewModel.loadPosts()
            findNavController().navigateUp()
        }
    }

    private fun setupButtons() {
        binding.remove.setOnClickListener {
            viewModel.removePhoto()
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .cameraOnly()
                .crop()
                .createIntent(photoLauncher::launch)
        }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()
                .crop()
                .createIntent(photoLauncher::launch)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val text = binding.edit.text.toString().trim()
                    if (text.isNotEmpty()) {
                        viewModel.saveDraft(text)
                    }
                    hideKeyboard()
                    findNavController().navigateUp()
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()

        if (!suppressNextDraftSave) {
            val text = binding.edit.text.toString().trim()
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
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}