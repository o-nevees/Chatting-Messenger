package com.data.source.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que armazena os detalhes de um grupo.
 * Serve como cache local para informações como nome, ícone, etc.
 */
@Entity(tableName = "group_details")
data class GroupDetailsEntity(
    @PrimaryKey val groupId: Int,
    val groupName: String?,
    val groupIcon: String?,
)