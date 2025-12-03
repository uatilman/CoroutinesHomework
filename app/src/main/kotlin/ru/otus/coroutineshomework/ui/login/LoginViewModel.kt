package ru.otus.coroutineshomework.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.otus.coroutineshomework.ui.login.data.Credentials

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginApi: LoginApi) : ViewModel() {

    private val _stateFlow = MutableStateFlow<LoginViewState>(LoginViewState.Login())
    val state = _stateFlow.asStateFlow()

    /**
     * Login to the network
     * @param name user name
     * @param password user password
     */
    fun login(name: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loginFlow(name, password).collect { _stateFlow.emit(it) }
        }
    }

    /**
     * Logout from the network
     */
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            logoutFlow().collect { _stateFlow.emit(it) }
        }
    }

    private fun loginFlow(name: String, password: String): Flow<LoginViewState> = flow {
        emit(LoginViewState.LoggingIn)
        runCatching {
            val user = loginApi.login(Credentials(name, password))
            emit(LoginViewState.Content(user))
        }.onFailure {
            emit(LoginViewState.Login(it as? Exception))
        }
    }

    private fun logoutFlow(): Flow<LoginViewState> = flow {
        emit(LoginViewState.LoggingOut)
        runCatching {
            loginApi.logout()
            emit(LoginViewState.Login())
        }.onFailure {
            emit(LoginViewState.Login(it as? Exception))
        }
    }
}
