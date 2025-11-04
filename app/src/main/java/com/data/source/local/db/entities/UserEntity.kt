package com.data.source.local.db.entities

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa um usuário (contato ou membro de grupo) no banco de dados local.
 * Esta tabela servirá como um cache local para os detalhes dos usuários,
 * permitindo que a UI exiba nomes e fotos de forma rápida e eficiente.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey @NonNull var number: String,
    var username1: String?, // Nome principal de exibição
    var username2: String?, // Nome de usuário (@)
    var lastOnline: String?,
    var profilePhoto: String?, // URL da foto do perfil
    
)