package com.data.repository

import android.app.Application
import android.util.Log
import com.data.source.local.db.AppDatabase
import com.data.source.local.db.dao.UserDao
import com.data.source.local.db.entities.UserEntity
import com.chatting.ui.utils.SecurePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gerenciador central para os dados e estado do usuário logado.
 * É a fonte única de verdade para o perfil do usuário atual, observando
 * diretamente o banco de dados para fornecer dados reativos e consistentes.
 */
class UserDataStore(
    private val application: Application,
    private val userDao: UserDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Chave para armazenar o número do usuário logado de forma segura.
    private val LOGGED_IN_USER_NUMBER_KEY = "logged_in_user_number"

    // Armazena o número do usuário logado. A UI e o repositório reagirão a mudanças aqui.
    private val loggedInUserNumber = MutableStateFlow(SecurePrefs.getString(LOGGED_IN_USER_NUMBER_KEY, null))

    /**
     * Expõe o `UserEntity` do usuário logado como um StateFlow.
     * Utiliza `flatMapLatest` para reagir a mudanças no `loggedInUserNumber`.
     * Se o número for nulo (logout), emite nulo. Caso contrário, observa o usuário
     * correspondente no banco de dados, garantindo que a UI sempre tenha os dados mais recentes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val loggedInUser: StateFlow<UserEntity?> = loggedInUserNumber.flatMapLatest { number ->
        if (number == null) {
            flowOf(null) // Emite nulo se ninguém estiver logado
        } else {
            userDao.getUserAsFlow(number) // Observa o usuário do banco de dados
        }
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000), // Mantém o flow ativo por 5s após o último observador sair
        null // Valor inicial
    )

    /**
     * Retorna o número de telefone do usuário logado no momento.
     */
    fun getCurrentUserNumber(): String? {
        return loggedInUserNumber.value
    }

    /**
     * Chamado quando o login é bem-sucedido.
     * Salva o perfil completo do usuário no banco de dados e atualiza o estado de login,
     * persistindo o número do usuário para inicializações futuras do app.
     * @param user O objeto UserEntity contendo todos os dados do perfil.
     */
    fun onLoginSuccess(user: UserEntity) {
        scope.launch {
            userDao.insertOrUpdateUser(user)
            SecurePrefs.putString(LOGGED_IN_USER_NUMBER_KEY, user.number)
            loggedInUserNumber.value = user.number
            Log.i("UserDataStore", "Sessão iniciada e dados salvos para o usuário: ${user.number}")
        }
    }

    /**
     * Limpa a sessão do usuário no DataStore.
     * Remove o número do usuário das preferências seguras e define o flow `loggedInUserNumber` como nulo,
     * o que efetivamente desloga o usuário da perspectiva dos dados.
     */
    fun onLogout() {
        SecurePrefs.remove(LOGGED_IN_USER_NUMBER_KEY)
        loggedInUserNumber.value = null
        Log.i("UserDataStore", "Sessão do usuário finalizada no UserDataStore.")
    }

    companion object {
        @Volatile
        private var INSTANCE: UserDataStore? = null

        fun getInstance(application: Application): UserDataStore {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(application)
                INSTANCE ?: UserDataStore(application, db.userDao()).also { INSTANCE = it }
            }
        }
    }
}