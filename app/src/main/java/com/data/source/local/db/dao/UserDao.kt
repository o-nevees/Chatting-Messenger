package com.data.source.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.data.source.local.db.entities.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para a entidade UserEntity.
 * Define as operações de banco de dados para a tabela 'users'.
 */
@Dao
interface UserDao {

    /**
     * Insere um novo usuário ou atualiza um existente se a chave primária (número) já existir.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsers(users: List<UserEntity>) // Added for bulk sync

    @Query("SELECT * FROM users WHERE number = :number LIMIT 1")
    suspend fun getUserById(number: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): LiveData<List<UserEntity>>

    // Retorna o usuário como um Flow para observação reativa.
    @Query("SELECT * FROM users WHERE number = :number LIMIT 1")
    fun getUserAsFlow(number: String): Flow<UserEntity?>

    @Query("DELETE FROM users")
    suspend fun clearTable() // Added for full sync
}