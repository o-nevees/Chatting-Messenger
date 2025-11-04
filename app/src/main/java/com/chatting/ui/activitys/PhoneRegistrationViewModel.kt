package com.chatting.ui.activitys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

// Estado da UI (agora com fullPhoneNumber e isPhoneNumberValid)
data class PhoneRegistrationUiState(
    val countryDisplayText: String = "",
    val countryCode: String = "",
    val localPhoneNumber: String = "",
    val fullPhoneNumber: String = "",
    val phoneNumberError: String? = null,
    val isPhoneNumberValid: Boolean = false
)

class PhoneRegistrationViewModel(private val phoneNumberUtil: PhoneNumberUtil) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneRegistrationUiState())
    val uiState: StateFlow<PhoneRegistrationUiState> = _uiState.asStateFlow()

    /**
     * Usado para a inicialização automática.
     * Preenche os dados iniciais e valida o número se ele existir.
     */
    fun setInitialData(countryDisplayText: String, countryCode: String, localNumber: String = "") {
        _uiState.update { currentState ->
            // Previne que dados já inseridos pelo usuário sejam sobrescritos
            if (currentState.countryCode.isEmpty() && currentState.localPhoneNumber.isEmpty()) {
                currentState.copy(
                    countryDisplayText = countryDisplayText,
                    countryCode = countryCode,
                    localPhoneNumber = localNumber
                )
            } else {
                currentState
            }
        }
        // Valida o número imediatamente se ele foi preenchido
        if (localNumber.isNotBlank()) {
            validatePhoneNumber()
        }
    }

    /**
     * Função para quando o USUÁRIO seleciona um país manualmente na lista.
     * Limpa o número de telefone antigo.
     */
    fun onCountrySelected(countryDisplayText: String, countryCode: String) {
        _uiState.update { currentState ->
            currentState.copy(
                countryDisplayText = countryDisplayText,
                countryCode = countryCode,
                localPhoneNumber = "", // Limpa o número antigo ao trocar de país
                fullPhoneNumber = "",
                phoneNumberError = null,
                isPhoneNumberValid = false
            )
        }
    }

    /**
     * Chamada quando o usuário digita o número.
     */
    fun onLocalPhoneNumberChange(newNumber: String) {
        _uiState.update { it.copy(localPhoneNumber = newNumber) }
        validatePhoneNumber()
    }

    private fun validatePhoneNumber() {
        viewModelScope.launch {
            val state = uiState.value
            val localNumber = state.localPhoneNumber
            val countryCode = state.countryCode

            if (localNumber.isBlank() || countryCode.isBlank()) {
                _uiState.update { it.copy(phoneNumberError = null, isPhoneNumberValid = false, fullPhoneNumber = "") }
                return@launch
            }

            try {
                // Remove o sinal '+' do código do país para a conversão
                val regionCode = phoneNumberUtil.getRegionCodeForCountryCode(countryCode.removePrefix("+").toInt())
                val parsedNumber: Phonenumber.PhoneNumber = phoneNumberUtil.parse(localNumber, regionCode)

                if (phoneNumberUtil.isValidNumberForRegion(parsedNumber, regionCode)) {
                    val formattedNumber = phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
                    _uiState.update {
                        it.copy(
                            phoneNumberError = null,
                            isPhoneNumberValid = true,
                            fullPhoneNumber = formattedNumber
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            phoneNumberError = "Número inválido para o país selecionado.",
                            isPhoneNumberValid = false,
                            fullPhoneNumber = ""
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        phoneNumberError = "Formato de número inválido.",
                        isPhoneNumberValid = false,
                        fullPhoneNumber = ""
                    )
                }
            }
        }
    }
}

class PhoneRegistrationViewModelFactory(private val phoneNumberUtil: PhoneNumberUtil) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhoneRegistrationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // O erro de digitação está aqui, corrigido para PhoneRegistrationViewModel
            return PhoneRegistrationViewModel(phoneNumberUtil) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}