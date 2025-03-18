package com.example.jewelleryapp.screen.loginScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.Repository.LoginScreenRepository
import com.example.jewelleryapp.Repository.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class LoginScreenViewModel(private val repository: LoginScreenRepository) : ViewModel() {
    // UI state
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // UI events
    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Handle email input changes
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    // Handle password input changes
    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    // Toggle password visibility
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    // Handle sign in button click
    fun onSignInClick() {
        if (_uiState.value.isLoading) return

        val email = _uiState.value.email
        val password = _uiState.value.password

        // Simple validation
        if (email.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(LoginUiEvent.ShowError("Email cannot be empty"))
            }
            return
        }

        if (password.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(LoginUiEvent.ShowError("Password cannot be empty"))
            }
            return
        }

        // Show loading and attempt login
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val result = repository.login(email, password)
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { user ->
                    _uiEvent.emit(LoginUiEvent.NavigateToHome(user))
                },
                onFailure = { error ->
                    _uiEvent.emit(LoginUiEvent.ShowError(error.message ?: "Login failed"))
                }
            )
        }
    }

    // Handle Google sign in button click
    fun onGoogleSignInClick() {
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val result = repository.loginWithGoogle()
            _uiState.value = _uiState.value.copy(isLoading = false)

            result.fold(
                onSuccess = { user ->
                    _uiEvent.emit(LoginUiEvent.NavigateToHome(user))
                },
                onFailure = { error ->
                    _uiEvent.emit(LoginUiEvent.ShowError(error.message ?: "Google login failed"))
                }
            )
        }
    }

    fun onForgotPasswordClick() {
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.NavigateToForgotPassword)
        }
    }

    fun onSignUpClick() {
        viewModelScope.launch {
            _uiEvent.emit(LoginUiEvent.NavigateToSignUp)
        }
    }
}

// UI State class
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

// UI Events sealed class
sealed class LoginUiEvent {
    data class NavigateToHome(val user: User) : LoginUiEvent()
    data class ShowError(val message: String) : LoginUiEvent()
    object NavigateToForgotPassword : LoginUiEvent()
    object NavigateToSignUp : LoginUiEvent()
}

// Factory for ViewModel
class LoginViewModelFactory(private val repository: LoginScreenRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginScreenViewModel::class.java)) {
            return LoginScreenViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}