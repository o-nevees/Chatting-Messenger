package com.chatting.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chatting.domain.AccountManager
import com.chatting.ui.MyApp
import com.data.source.local.db.entities.UserEntity
import com.data.repository.ChatRepository
import com.service.api.ApiService
import com.data.repository.UserDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.chatting.ui.utils.SecurePrefs
import java.lang.IllegalArgumentException

data class ProfileUiState(
    val user: UserEntity? = null,
    val name: String = "",
    val username: String = "",
    val about: String = "",
    val profileImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val snackbarMessage: String? = null
)

class ProfileViewModel(
    application: Application,
    private val accountManager: AccountManager
) : AndroidViewModel(application) {

    private val userDataStore = UserDataStore.getInstance(application)

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val user = userDataStore.loggedInUser.first()
            _uiState.update {
                it.copy(
                    user = user,
                    name = user?.username1 ?: "",
                    username = user?.username2 ?: "",
                    about = ""
                )
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun onAboutChange(newAbout: String) {
        _uiState.update { it.copy(about = newAbout) }
    }

    fun onEditToggle() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value

            if (currentState.name.isBlank() || currentState.username.isBlank()) {
                _uiState.update { it.copy(isLoading = false, snackbarMessage = "Nome e nome de usuário não podem estar vazios.") }
                return@launch
            }
             if (!currentState.username.startsWith("@") || currentState.username.length < 5) {
                 _uiState.update { it.copy(isLoading = false, snackbarMessage = "Nome de usuário inválido.") }
                 return@launch
             }

            val result = accountManager.saveProfile(
                name = currentState.name,
                username = currentState.username,
                bio = currentState.about
            )

            if (result.isSuccess) {
                Log.i(TAG, "Perfil atualizado com sucesso.")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditing = false,
                        snackbarMessage = result.getOrNull()?.message ?: "Perfil atualizado!"
                    )
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                Log.e(TAG, "Falha ao atualizar perfil: $errorMsg")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = "Falha ao atualizar: $errorMsg"
                    )
                }
            }
        }
    }
}

class ProfileViewModelFactory(
    private val application: Application,
    private val accountManager: AccountManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application, accountManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}