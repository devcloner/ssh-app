package com.example.data.repository

import com.example.data.local.ChatMessageDao
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatMessageDao: ChatMessageDao) {
    fun getMessagesForHost(hostId: Long): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesForHost(hostId)

    suspend fun insertMessage(message: ChatMessage): Long =
        chatMessageDao.insertMessage(message)

    suspend fun updateMessage(message: ChatMessage) =
        chatMessageDao.updateMessage(message)

    suspend fun clearMessagesForHost(hostId: Long) =
        chatMessageDao.clearMessagesForHost(hostId)
}
