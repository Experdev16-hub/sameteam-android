package com.example.sameteam.quickBlox

import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogMessageListener
import com.quickblox.chat.model.QBChatMessage


open class QbChatDialogMessageListenerImpl : QBChatDialogMessageListener {

    override fun processMessage(s: String, qbChatMessage: QBChatMessage, integer: Int?) {

    }

    override fun processError(s: String, e: QBChatException, qbChatMessage: QBChatMessage, integer: Int?) {

    }
}