package com.data.source.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.data.source.local.db.entities.GroupDetailsEntity
import com.data.source.local.db.entities.GroupMemberEntity

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGroupDetails(groupDetails: GroupDetailsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGroups(groups: List<GroupDetailsEntity>) // Added for bulk sync

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Changed to REPLACE for sync
    suspend fun insertGroupMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearGroupMembers(groupId: Int)

    /**
     * Transação para atualizar um grupo e sua lista de membros de forma atômica.
     */
    @Transaction
    suspend fun updateGroupAndMembers(groupDetails: GroupDetailsEntity, members: List<GroupMemberEntity>) {
        insertOrUpdateGroupDetails(groupDetails)
        // A estratégia REPLACE em insertGroupMembers torna clear desnecessário se a lista 'members' for completa
        // clearGroupMembers(groupDetails.groupId) // Pode ser removido se insertGroupMembers usar REPLACE
        insertGroupMembers(members) // Garante que todos os membros atuais sejam inseridos/atualizados
    }

    // Retorna todos os detalhes de grupos como LiveData
    @Query("SELECT * FROM group_details")
    fun getAllGroupDetails(): LiveData<List<GroupDetailsEntity>>

    // Busca detalhes de um grupo específico
    @Query("SELECT * FROM group_details WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroupDetailsById(groupId: Int): GroupDetailsEntity? // Added for sync check

   
    // Deleta todos os detalhes de grupos
    @Query("DELETE FROM group_details")
    suspend fun clearGroupDetailsTable() // Added for full sync

    // Deleta todos os membros de grupos
    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembersTable() // Added for full sync
}