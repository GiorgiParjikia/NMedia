package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInViewModel

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)

        binding.signInButton.setOnClickListener {
            val login = binding.loginField.text?.toString().orEmpty()
            val pass = binding.passwordField.text?.toString().orEmpty()
            viewModel.signIn(login, pass)
        }

        binding.toSignUp.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.progress.visibility = if (state.loading) View.VISIBLE else View.GONE

            state.error?.let {
                binding.errorText.text = it
                binding.errorText.visibility = View.VISIBLE
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            } ?: run {
                binding.errorText.visibility = View.GONE
            }
        }

        viewModel.authSuccess.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}