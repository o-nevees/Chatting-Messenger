package com.chatting.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// import com.chatting.network.ApiClient // Removido
import com.service.api.ApiService // Adicionado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

data class CreateProfileUiState(
    val currentStep: Int = 0,
    val name: String = "",
    val birthdate: String = "",
    val username: String = "",
    val profileImageUri: Uri? = null,
    val usernameValidationState: UsernameValidationState = UsernameValidationState.Idle,
    val isNextButtonEnabled: Boolean = false,
    val isLoading: Boolean = false
)

enum class UsernameValidationState {
    Idle, Checking, Available, Taken, TooShort
}

class CreateProfileViewModel(
    private val apiService: ApiService // Recebe ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var validationJob: Job? = null
    private val totalSteps = 4
    companion object {
        private const val TAG = "CreateProfileViewModel"
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
        validateCurrentStep()
    }

    fun onBirthdateChange(newDate: String) {
        _uiState.update { it.copy(birthdate = newDate) }
        validateCurrentStep()
    }

    fun onUsernameChange(newUsername: String) {
        val formattedUsername = if (newUsername.isNotEmpty() && !newUsername.startsWith("@")) {
            "@${newUsername.replace("@", "")}"
        } else {
            newUsername
        }

        _uiState.update { it.copy(username = formattedUsername) }
        validateUsername(formattedUsername)
    }

    fun onProfileImageChange(uri: Uri?) {
        _uiState.update { it.copy(profileImageUri = uri) }
        validateCurrentStep()
    }

    fun onNextStep() {
        if (_uiState.value.currentStep < totalSteps - 1) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
            validateCurrentStep()
        }
    }

    fun onPreviousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
            validateCurrentStep()
        }
    }

    fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading) }
    }


    private fun validateUsername(username: String) {
        validationJob?.cancel()

        if (username.length < 5 || !username.startsWith('@') || !username.matches(Regex("@[a-zA-Z0-9_]{4,}"))) {
            _uiState.update { it.copy(usernameValidationState = UsernameValidationState.TooShort) }
            validateCurrentStep()
            return
        }

        _uiState.update { it.copy(usernameValidationState = UsernameValidationState.Checking) }
        validateCurrentStep()

        validationJob = viewModelScope.launch {
            delay(500)
            try {
                Log.d(TAG, "Chamando API /profile/checkUsername para: $username")
                // Usa a instância injetada do apiService
                val response = apiService.checkUsername(username = username)
                val body = response.body()

                
                if (response.isSuccessful && body != null && body.status == "success" && body.data != null) {
                    
                    val data = body.data
                    
                    // ✅ CORREÇÃO AQUI (Linha 116): Adicionado '?.' (safe call) e '?: false' (valor padrão)
                    val isAvailable = data?.available ?: false
                    Log.d(TAG, "Resposta /profile/checkUsername: available=$isAvailable")
                    _uiState.update {
                        it.copy(usernameValidationState = if (isAvailable) UsernameValidationState.Available else UsernameValidationState.Taken)
                    }
                } else {
                    val errorMsg = body?.message ?: "Erro ${response.code()}"
                    Log.e(TAG, "Erro na API /profile/checkUsername: $errorMsg")
                    _uiState.update { it.copy(usernameValidationState = UsernameValidationState.Taken) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro de rede ao validar username", e)
                _uiState.update { it.copy(usernameValidationState = UsernameValidationState.Idle) }
            } finally {
                validateCurrentStep()
            }
        }
    }

    private fun validateCurrentStep() {
        val state = _uiState.value
        val isStepValid = when (state.currentStep) {
            0 -> state.name.isNotBlank()
            1 -> state.birthdate.isNotBlank()
            2 -> state.usernameValidationState == UsernameValidationState.Available
            3 -> true
            else -> false
        }
        _uiState.update { it.copy(isNextButtonEnabled = isStepValid) }
    }
}

// Factory para injetar o ApiService
class CreateProfileViewModelFactory(
    private val apiService: ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateProfileViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}