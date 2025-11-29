package ru.otus.coroutineshomework.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.otus.coroutineshomework.databinding.ContentBinding
import ru.otus.coroutineshomework.databinding.FragmentLoginBinding
import ru.otus.coroutineshomework.databinding.LoadingBinding
import ru.otus.coroutineshomework.databinding.LoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var login: LoginBinding
    private lateinit var content: ContentBinding
    private lateinit var loading: LoadingBinding

    private val loginViewModel by viewModels<LoginViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        login = binding.loginScreen
        content = binding.contentScreen
        loading = binding.loadingScreen

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLogin()
        setupContent()
        loginViewModel.state.onEach {
            when (it) {
                is LoginViewState.Login -> showLogin(it)
                LoginViewState.LoggingIn -> showLoggingIn()
                is LoginViewState.Content -> showContent(it)
                LoginViewState.LoggingOut -> showLoggingOut()
            }
        }.launchIn(lifecycleScope)
    }

    private fun setupLogin() {
        login.loginButton.setOnClickListener {
            val name = login.login.text?.trim() ?: ""
            val password = login.password.text?.trim() ?: ""

            loginViewModel.login(name.toString(), password.toString())
        }
    }

    private fun showLogin(data: LoginViewState.Login) {
        login.loginGroup.isVisible = true
        login.loginLayout.isEnabled = true
        login.loginLayout.error = data.error?.let { it.message ?: it.toString() }
        login.passwordLayout.isEnabled = true
        login.loginButton.isEnabled = true
        login.loggingIn.isVisible = false

        content.contentGroup.isVisible = false

        loading.loadingGroup.isVisible = false
    }

    private fun showLoggingIn() {
        login.loginGroup.isVisible = true
        login.loginLayout.isEnabled = false
        login.loginLayout.error = null
        login.passwordLayout.isEnabled = false
        login.loginButton.isEnabled = false
        login.loggingIn.isVisible = true

        content.contentGroup.isVisible = false

        loading.loadingGroup.isVisible = false
    }

    private fun showContent(data: LoginViewState.Content) {
        login.loginGroup.isVisible = false

        content.contentGroup.isVisible = true
        content.name.text = data.user.name
    }

    private fun showLoggingOut() {
        login.loginGroup.isVisible = false
        content.contentGroup.isVisible = false
        loading.loadingGroup.isVisible = true
    }

    private fun setupContent() {
        content.logoutButton.setOnClickListener {
            loginViewModel.logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}