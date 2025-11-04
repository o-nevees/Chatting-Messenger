package com.data.source.local.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entidade que representa a relação entre Grupos e Usuários.
 * Corresponde à tabela `group_members` do backend.
 */
@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "userNumber"], // Chave primária composta
    indices = [Index(value = ["userNumber"])],
    foreignKeys = [
        ForeignKey(
            entity = GroupDetailsEntity::class,
            parentColumns = ["groupId"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupMemberEntity(
    val groupId: Int,
    val userNumber: String // Número de telefone do membro
)