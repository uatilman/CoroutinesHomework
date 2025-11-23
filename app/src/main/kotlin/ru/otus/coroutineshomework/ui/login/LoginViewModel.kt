package ru.otus.coroutineshomework.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.otus.coroutineshomework.ui.login.data.Credentials

class LoginViewModel : ViewModel() {

    private val _state = MutableLiveData<LoginViewState>(LoginViewState.Login())
    val state: LiveData<LoginViewState> = _state

    /**
     * Login to the network
     * @param name user name
     * @param password user password
     */
    fun login(name: String, password: String) {
        _state.postValue(LoginViewState.LoggingIn)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val user = LoginApi().login(Credentials(name, password))
                _state.postValue(LoginViewState.Content(user))
            }.onFailure {
                _state.postValue(LoginViewState.Login(it as? Exception))
            }

        }
    }

    /**
     * Logout from the network
     */
    fun logout() {
        _state.postValue(LoginViewState.LoggingOut)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                LoginApi().logout()
                _state.postValue(LoginViewState.Login())
            }.onFailure {
                _state.postValue(LoginViewState.Login(it as? Exception))
            }
        }
    }
}
