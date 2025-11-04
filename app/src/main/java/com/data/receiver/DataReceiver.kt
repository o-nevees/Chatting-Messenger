package com.data.receiver

/**
 * Interface que define o contrato para processar dados brutos
 * recebidos do WebSocket.
 */
interface DataReceiver {

    /** Processa o payload de 'sync_data' (full_sync ou event_sync). */
    suspend fun processSyncData(jsonData: String)

    /** Processa uma nova mensagem ('new_message'). */
    suspend fun processIncomingMessage(command: String, jsonData: String)

    /** Processa uma edição de mensagem ('edit_msg', 'edit_msg_group'). */
    suspend fun processEditMessage(command: String, jsonData: String)

    /** Processa uma exclusão de mensagem ('delete_msg', 'delete_msg_group'). */
    suspend fun processDeleteMessage(command: String, jsonData: String)

    /** Processa uma atualização de status vinda do servidor ('update_message_status'). */
    suspend fun processMessageStatusUpdateFromServer(payload: String)

    /** Processa um recibo de leitura de outro usuário ('message_read_receipt'). */
    suspend fun processMessageReadReceiptFromServer(payload: String)

    /** Processa uma atualização de status online de um usuário ('is_user_online'). */
    suspend fun processOnlineStatusUpdate(payload: String)
}