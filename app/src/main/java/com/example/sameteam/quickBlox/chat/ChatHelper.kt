
package com.example.sameteam.quickBlox.chat


import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.QBRequestGetBuilder
import com.quickblox.users.model.QBUser
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.example.sameteam.*
import com.example.sameteam.quickBlox.*
import com.quickblox.auth.session.QBSettings
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBRestChatService
import com.quickblox.chat.request.QBDialogRequestBuilder
import com.quickblox.chat.request.QBMessageGetBuilder
import com.quickblox.content.QBContent
import com.quickblox.content.model.QBFile
import com.quickblox.core.LogLevel
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.QBProgressCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.core.request.QBPagedRequestBuilder
import com.quickblox.core.request.QBRequestGetBuilder
import com.example.sameteam.quickBlox.callback.QbEntityCallbackTwoTypeWrapper
import com.example.sameteam.quickBlox.callback.QbEntityCallbackWrapper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.quickblox.chat.model.*
import com.quickblox.core.request.QBRequestUpdateBuilder
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smackx.muc.DiscussionHistory
import java.io.File

const val CHAT_HISTORY_ITEMS_PER_PAGE = 50
const val USERS_PER_PAGE = 100
const val TOTAL_PAGES_BUNDLE_PARAM = "total_pages"
const val CURRENT_PAGE_BUNDLE_PARAM = "current_page"
private const val CHAT_HISTORY_ITEMS_SORT_FIELD = "date_sent"

object ChatHelper {
    private val TAG = ChatHelper::class.java.simpleName

    private var qbChatService: QBChatService = QBChatService.getInstance()
    private var usersLoadedFromDialog = ArrayList<QBUser>()
    private var usersLoadedFromDialogs = ArrayList<QBUser>()
    private var usersLoadedFromMessage = ArrayList<QBUser>()
    private var usersLoadedFromMessages = ArrayList<QBUser>()
    private var loginInProgress = false

    
    init {
        //TODO Remove debug logs
        QBSettings.getInstance().logLevel = LogLevel.DEBUG
        QBChatService.setDebugEnabled(true)
        QBChatService.setConfigurationBuilder(buildChatConfigs())
        QBChatService.setDefaultPacketReplyTimeout(10000000)
        qbChatService.setUseStreamManagement(true)
    }

    private fun buildChatConfigs(): QBChatService.ConfigurationBuilder {
        val configurationBuilder = QBChatService.ConfigurationBuilder()

        configurationBuilder.socketTimeout = SOCKET_TIMEOUT
        configurationBuilder.isUseTls = USE_TLS
        configurationBuilder.isKeepAlive = KEEP_ALIVE
        configurationBuilder.isAutojoinEnabled = AUTO_JOIN
        configurationBuilder.setAutoMarkDelivered(AUTO_MARK_DELIVERED)
        configurationBuilder.isReconnectionAllowed = RECONNECTION_ALLOWED
        configurationBuilder.setAllowListenNetwork(ALLOW_LISTEN_NETWORK)
        configurationBuilder.port = CHAT_PORT

        return configurationBuilder
    }

    private fun buildDialogNameWithoutUser(dialogName: String, userName: String): String {
        val regex = ", $userName|$userName, "
        return dialogName.replace(regex.toRegex(), "")
    }

    fun isLogged(): Boolean {
        return QBChatService.getInstance().isLoggedIn
    }

    fun getCurrentUser(): QBUser? {
        if (SharedPrefsHelper.hasQbUser()) {
            return SharedPrefsHelper.getQbUser()!!
        } else {
            return null
        }
    }

    fun addConnectionListener(listener: ConnectionListener?) {
        qbChatService.addConnectionListener(listener)
    }

    fun removeConnectionListener(listener: ConnectionListener?) {
        qbChatService.removeConnectionListener(listener)
    }

    fun updateUser(user: QBUser, callback: QBEntityCallback<QBUser>) {
        QBUsers.updateUser(user).performAsync(object : QBEntityCallback<QBUser> {
            override fun onSuccess(user: QBUser, bundle: Bundle) {
                callback.onSuccess(user, bundle)
            }

            override fun onError(e: QBResponseException) {
                callback.onError(e)
            }
        })
    }

    fun login(user: QBUser, callback: QBEntityCallback<QBUser>) {
        // Create REST API session on QuickBlox
        Log.e("LOGIN QUICK", "LOGIN QUICK")

        QBUsers.signIn(user)
            .performAsync(object : QbEntityCallbackTwoTypeWrapper<QBUser, QBUser>(callback) {
                override fun onSuccess(t: QBUser, bundle: Bundle?) {
                    callback.onSuccess(t, bundle)
                }
            })
    }

fun loginToChat(user: QBUser, callback: QBEntityCallback<Void>) {
    if (loginInProgress) {
        callback.onError(QBResponseException("Login already in progress"))
        return
    }
    
    if (!qbChatService.isLoggedIn) {
        loginInProgress = true
        qbChatService.login(user, object : QBEntityCallback<Void> {
            override fun onSuccess(result: Void?, bundle: Bundle?) {
                loginInProgress = false
                callback.onSuccess(result, bundle)
            }

            override fun onError(e: QBResponseException) {
                loginInProgress = false
                callback.onError(e)
            }
        })
    } else {
        callback.onSuccess(null, null)
    }
} // ADDED MISSING CLOSING BRACE

    fun join(chatDialog: QBChatDialog, callback: QBEntityCallback<Void>) {
        val history = DiscussionHistory()
        history.maxStanzas = 0
        chatDialog.join(history, callback)
    }

    @Throws(Exception::class)
    fun join(dialogs: List<QBChatDialog>) {
        for (dialog in dialogs) {
            val history = DiscussionHistory()
            history.maxStanzas = 0
            dialog.join(history)
        }
    }

    @Throws(XMPPException::class, SmackException.NotConnectedException::class)
    fun leaveChatDialog(chatDialog: QBChatDialog) {
        chatDialog.leave()
    }

    fun destroy() {
        qbChatService.destroy()
    }

    fun createDialogWithSelectedUsers(
        users: MutableList<QBUser>, chatName: String, groupPhoto: String?, desc: String?,
        hasTask: Boolean?, isPrivate: Boolean?, callback: QBEntityCallback<QBChatDialog>
    ) {
        val dialog = createDialog(users, chatName, isPrivate)
        if (!groupPhoto.isNullOrBlank()) dialog.photo = groupPhoto
        val customData = QBDialogCustomData("QBTeam")
        if (!desc.isNullOrBlank()) {
            customData.putString("description", desc)
        }
        val currentUser = getCurrentUser()
        if (currentUser?.id != null) {
            customData.putInteger("admin", currentUser.id)
        }
        if (hasTask != null)
            customData.putBoolean("hasTask", hasTask)
        else
            customData.putBoolean("hasTask", false)

        dialog.customData = customData

        QBRestChatService.createChatDialog(dialog)
            .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
                override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
                    QbDialogHolder.addDialog(t)
                    val newUsers = ArrayList(users)
                    QbUsersDbManager.saveAllUsers(newUsers, false)
                    super.onSuccess(t, bundle)
                }
            })
    }

    fun deletePrivateDialogs(
        privateDialogsToDelete: List<QBChatDialog>,
        callback: QBEntityCallback<ArrayList<String>>
    ) {
        if (privateDialogsToDelete.isNotEmpty()) {
            val privateDialogsIds = StringifyArrayList<String>()
            for (privateDialog in privateDialogsToDelete) {
                privateDialogsIds.add(privateDialog.dialogId)
            }
            QBRestChatService.deleteDialogs(privateDialogsIds, false, null)
                .performAsync(object : QBEntityCallback<ArrayList<String>> {
                    override fun onSuccess(
                        deletedDialogs: java.util.ArrayList<String>,
                        bundle: Bundle?
                    ) {
                        callback.onSuccess(deletedDialogs, bundle)
                    }

                    override fun onError(e: QBResponseException) {
                        callback.onError(e)
                    }
                })
        }
    }

    fun leaveGroupDialogs(
        groupDialogsToDelete: List<QBChatDialog>,
        callback: QBEntityCallback<List<QBChatDialog>>
    ) {
        if (groupDialogsToDelete.isNotEmpty()) {
            DeleteGroupDialogsTask(groupDialogsToDelete, callback).execute()
        }
    }

    fun deleteDialog(qbDialog: QBChatDialog, callback: QBEntityCallback<Void>) {
        QBRestChatService.deleteDialog(qbDialog.dialogId, true).performAsync(
            QbEntityCallbackWrapper(callback)
        )
        try {
            if (QbDialogHolder.hasDialogWithId(qbDialog.dialogId))
                QbDialogHolder.deleteDialog(qbDialog)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exitFromDialog(qbDialog: QBChatDialog, callback: QBEntityCallback<QBChatDialog>) {
        try {
            leaveChatDialog(qbDialog)
        } catch (e: XMPPException) {
            callback.onError(QBResponseException(e.message))
        } catch (e: SmackException.NotConnectedException) {
            callback.onError(QBResponseException(e.message))
        }
        val currentUser = QBChatService.getInstance().user
        val qbRequestBuilder = QBDialogRequestBuilder()
        qbRequestBuilder.removeUsers(currentUser.id)
        qbDialog.name = buildDialogNameWithoutUser(qbDialog.name, currentUser.fullName)
        QBRestChatService.updateChatDialog(qbDialog, qbRequestBuilder).performAsync(callback)
    }

    fun updateDialog(
        qbDialog: QBChatDialog,
        newQbDialogUsersList: List<QBUser>, groupName: String?, groupPhoto: String?, desc: String?,
        changeDescription: Boolean,
        callback: QBEntityCallback<QBChatDialog>
    ) {
        val addedUsers = getAddedUsers(qbDialog, newQbDialogUsersList)
        val removedUsers = getRemovedUsers(qbDialog, newQbDialogUsersList)

        if (!changeDescription) {
            Log.d(TAG, "changeDescription value: if")

            var requestQBChatDialog = QBChatDialog()
            requestQBChatDialog.dialogId = qbDialog.dialogId
            if (!groupName.isNullOrBlank()) requestQBChatDialog.name = groupName
            if (!groupPhoto.isNullOrBlank()) requestQBChatDialog.photo = groupPhoto

            val qbRequestBuilder = QBDialogRequestBuilder()
            if (addedUsers.isNotEmpty()) {
                qbRequestBuilder.addUsers(*addedUsers.toTypedArray())
            }
            if (removedUsers.isNotEmpty()) {
                qbRequestBuilder.removeUsers(*removedUsers.toTypedArray())
            }

            QBRestChatService.updateChatDialog(requestQBChatDialog, qbRequestBuilder)
                .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
                    override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
                        val newUsers = ArrayList(newQbDialogUsersList)
                        QbUsersDbManager.saveAllUsers(newUsers, false)
                        logDialogUsers(t)
                        super.onSuccess(t, bundle)
                    }
                })
        } else {
            Log.d(TAG, "changeDescription value: else")

            var requestQBChatDialog = QBChatDialog()
            requestQBChatDialog.dialogId = qbDialog.dialogId
            if (!groupName.isNullOrBlank()) requestQBChatDialog.name = groupName
            if (!groupPhoto.isNullOrBlank()) requestQBChatDialog.photo = groupPhoto

            val qbRequestBuilder = QBDialogRequestBuilder()
            if (addedUsers.isNotEmpty()) {
                qbRequestBuilder.addUsers(*addedUsers.toTypedArray())
            }
            if (removedUsers.isNotEmpty()) {
                qbRequestBuilder.removeUsers(*removedUsers.toTypedArray())
            }

            val customData = QBDialogCustomData("QBTeam")
            customData.putString("description", desc)
            requestQBChatDialog.customData = customData

            val currentUser = getCurrentUser()
            if (currentUser?.id != null) {
                customData.putInteger("admin", currentUser.id)
                requestQBChatDialog.customData = customData
            }

            Log.d(TAG, "QbChat Dialog: ${requestQBChatDialog.toString()}")

            QBRestChatService.updateChatDialog(requestQBChatDialog, qbRequestBuilder)
                .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
                    override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
                        requestQBChatDialog.customData = null

                        Log.d(TAG, "QbChat Dialog 2: ${requestQBChatDialog.toString()}")

                        QBRestChatService.updateChatDialog(requestQBChatDialog, qbRequestBuilder)
                            .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
                                override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
                                    val newUsers = ArrayList(newQbDialogUsersList)
                                    QbUsersDbManager.saveAllUsers(newUsers, false)
                                    logDialogUsers(t)
                                    super.onSuccess(t, bundle)
                                }
                            })
                    }
                })


        }

        logDialogUsers(qbDialog)
        logUsers(addedUsers)
        Log.w(TAG, "=======================")
        logUsers(removedUsers)

//        val qbRequestBuilder = QBDialogRequestBuilder()
//        if (addedUsers.isNotEmpty()) {
//            qbRequestBuilder.addUsers(*addedUsers.toTypedArray())
//        }
//        if (removedUsers.isNotEmpty()) {
//            qbRequestBuilder.removeUsers(*removedUsers.toTypedArray())
//        }
//
//        if (!groupName.isNullOrBlank()) qbDialog.name = groupName
//        if (!groupPhoto.isNullOrBlank()) qbDialog.photo = groupPhoto
//        val customData = QBDialogCustomData("QBTeam")
//        if (!desc.isNullOrBlank()) {
//            customData.putString("description", desc)
//            qbDialog.customData = customData
//        }
//
//        val currentUser = getCurrentUser()
//        if (currentUser?.id != null) {
//            customData.putInteger("admin", currentUser.id)
//            qbDialog.customData = customData
//        }

//        QBRestChatService.updateChatDialog(qbDialog, qbRequestBuilder)
//            .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
//                override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
//                    val newUsers = ArrayList(newQbDialogUsersList)
//                    QbUsersDbManager.saveAllUsers(newUsers, false)
//                    logDialogUsers(t)
//                    super.onSuccess(t, bundle)
//                }
//            })
    }

    fun addTaskToDialog(
        qbDialog: QBChatDialog,
        taskId: Int,
        callback: QBEntityCallback<QBChatDialog>
    ) {
        val customData = QBDialogCustomData("QBTeam")
        customData.putInteger("taskId", taskId)
        customData.putBoolean("hasTask", true)
        qbDialog.customData = customData

        val qbRequestBuilder = QBDialogRequestBuilder()

        QBRestChatService.updateChatDialog(qbDialog, qbRequestBuilder)
            .performAsync(object : QbEntityCallbackWrapper<QBChatDialog>(callback) {
                override fun onSuccess(t: QBChatDialog, bundle: Bundle?) {
                    super.onSuccess(t, bundle)
                }
            })
    }

    fun updateTeamImages(
        qbDialog: QBChatDialog,
        customData: QBDialogCustomData,
        callback: QBEntityCallback<QBChatDialog>
    ) {
        val requestBuilder = QBRequestUpdateBuilder()
        qbDialog.customData = customData

        QBRestChatService.updateChatDialog(qbDialog, requestBuilder).performAsync(callback)
    }

    fun loadChatHistory(
        dialog: QBChatDialog,
        skipPagination: Int,
        callback: QBEntityCallback<ArrayList<QBChatMessage>>
    ) {
        val messageGetBuilder = QBMessageGetBuilder()
        messageGetBuilder.skip = skipPagination
        messageGetBuilder.limit = CHAT_HISTORY_ITEMS_PER_PAGE
        messageGetBuilder.sortDesc(CHAT_HISTORY_ITEMS_SORT_FIELD)
        messageGetBuilder.markAsRead(false)

        QBRestChatService.getDialogMessages(dialog, messageGetBuilder)
            .performAsync(object : QbEntityCallbackWrapper<ArrayList<QBChatMessage>>(callback) {
                override fun onSuccess(t: ArrayList<QBChatMessage>, bundle: Bundle?) {
                    val userIds = HashSet<Int>()
                    for (message in t) {
                        userIds.add(message.senderId)
                    }
                    if (userIds.isNotEmpty()) {
                        getUsersFromMessages(t, userIds, callback)
                    } else {
                        callback.onSuccess(t, bundle)
                    }
                    // Not calling super.onSuccess() because
                    // we're want to load chat userList before triggering the callback
                }
            })
    }

    fun getDialogs(
        requestBuilder: QBRequestGetBuilder,
        callback: QBEntityCallback<ArrayList<QBChatDialog>>
    ) {

        QBRestChatService.getChatDialogs(null, requestBuilder).performAsync(
            object : QbEntityCallbackWrapper<ArrayList<QBChatDialog>>(callback) {
                override fun onSuccess(dialogs: ArrayList<QBChatDialog>, bundle: Bundle?) {
                    getUsersFromDialogs(dialogs, callback)
                    // Not calling callback.onSuccess(...) because
                    // we want to load chat userList before triggering callback
                }
            })
    }

    fun getDialogById(dialogId: String, callback: QBEntityCallback<QBChatDialog>) {
        QBRestChatService.getChatDialogById(dialogId).performAsync(callback)
    }

    fun getUsersFromDialog(dialog: QBChatDialog, callback: QBEntityCallback<ArrayList<QBUser>>) {
        val userIds = dialog.occupants
        val requestBuilder = QBPagedRequestBuilder(USERS_PER_PAGE, 1)
        usersLoadedFromDialog.clear()
        loadUsersByIDsFromDialog(userIds, requestBuilder, callback)
    }


    private fun loadUsersByIDsFromDialogs(
        userIDs: Collection<Int>,
        requestBuilder: QBPagedRequestBuilder,
        callback: QBEntityCallback<ArrayList<QBUser>>
    ) {
        QBUsers.getUsersByIDs(userIDs, requestBuilder)
            .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                override fun onSuccess(qbUsers: ArrayList<QBUser>?, bundle: Bundle?) {
                    if (qbUsers != null) {
                        usersLoadedFromDialogs.addAll(qbUsers)
                        QbUsersDbManager.saveAllUsers(qbUsers, false)
                        bundle?.let {
                            val totalPages = it.get(TOTAL_PAGES_BUNDLE_PARAM) as Int
                            val currentPage = it.get(CURRENT_PAGE_BUNDLE_PARAM) as Int
                            if (totalPages > currentPage) {
                                requestBuilder.page = currentPage + 1
                                loadUsersByIDsFromDialogs(userIDs, requestBuilder, callback)
                            } else {
                                callback.onSuccess(usersLoadedFromDialogs, bundle)
                            }
                        }
                    }
                }

                override fun onError(e: QBResponseException?) {
                    callback.onError(e)
                }
            })
    }

    private fun getUsersFromMessages(
        messages: ArrayList<QBChatMessage>,
        userIds: Set<Int>,
        callback: QBEntityCallback<ArrayList<QBChatMessage>>
    ) {

        val requestBuilder = QBPagedRequestBuilder(USERS_PER_PAGE, 1)
        usersLoadedFromMessages.clear()
        loadUsersByIDsFromMessages(
            userIds,
            requestBuilder,
            object : QBEntityCallback<ArrayList<QBUser>> {
                override fun onSuccess(qbUsers: ArrayList<QBUser>?, b: Bundle?) {
                    callback.onSuccess(messages, b)
                }

                override fun onError(e: QBResponseException?) {
                    callback.onError(e)
                }
            })
    }

    private fun loadUsersByIDsFromMessages(
        userIDs: Collection<Int>,
        requestBuilder: QBPagedRequestBuilder,
        callback: QBEntityCallback<ArrayList<QBUser>>
    ) {
        QBUsers.getUsersByIDs(userIDs, requestBuilder)
            .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                override fun onSuccess(qbUsers: ArrayList<QBUser>?, bundle: Bundle?) {
                    if (qbUsers != null) {
                        usersLoadedFromMessages.addAll(qbUsers)
                        QbUsersDbManager.saveAllUsers(qbUsers, false)
                        bundle?.let {
                            val totalPages = it.get(TOTAL_PAGES_BUNDLE_PARAM) as Int
                            val currentPage = it.get(CURRENT_PAGE_BUNDLE_PARAM) as Int
                            if (totalPages > currentPage) {
                                requestBuilder.page = currentPage + 1
                                loadUsersByIDsFromMessages(userIDs, requestBuilder, callback)
                            } else {
                                callback.onSuccess(usersLoadedFromMessages, bundle)
                            }
                        }
                    }
                }

                override fun onError(e: QBResponseException?) {
                    callback.onError(e)
                }
            })
    }

    fun getUsersFromMessage(message: QBChatMessage, callback: QBEntityCallback<ArrayList<QBUser>>) {
        val userIds = ArrayList<Int>()
        val usersDelivered = message.deliveredIds
        val usersRead = message.readIds

        for (id in usersDelivered) {
            userIds.add(id)
        }
        for (id in usersRead) {
            userIds.add(id)
        }

        val requestBuilder = QBPagedRequestBuilder(USERS_PER_PAGE, 1)
        usersLoadedFromMessage.clear()
        loadUsersByIDsFromMessage(userIds, requestBuilder, callback)
    }

    private fun loadUsersByIDsFromMessage(
        userIDs: Collection<Int>,
        requestBuilder: QBPagedRequestBuilder,
        callback: QBEntityCallback<ArrayList<QBUser>>
    ) {
        QBUsers.getUsersByIDs(userIDs, requestBuilder)
            .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                override fun onSuccess(qbUsers: ArrayList<QBUser>?, bundle: Bundle?) {
                    if (qbUsers != null) {
                        usersLoadedFromMessage.addAll(qbUsers)
                        QbUsersDbManager.saveAllUsers(qbUsers, false)
                        bundle?.let {
                            val totalPages = it.get(TOTAL_PAGES_BUNDLE_PARAM) as Int
                            val currentPage = it.get(CURRENT_PAGE_BUNDLE_PARAM) as Int
                            if (totalPages > currentPage) {
                                requestBuilder.page = currentPage + 1
                                loadUsersByIDsFromMessage(userIDs, requestBuilder, callback)
                            } else {
                                callback.onSuccess(usersLoadedFromMessage, bundle)
                            }
                        }
                    }
                }

                override fun onError(e: QBResponseException?) {
                    callback.onError(e)
                }
            })
    }

    private class DeleteGroupDialogsTask internal constructor(
        private val groupDialogsToDelete: List<QBChatDialog>,
        private val callback: QBEntityCallback<List<QBChatDialog>>?
    ) : AsyncTask<Void, Void, Void>() {
        private var errorOccurs = false
        private val successfulDeletedDialogs = ArrayList<QBChatDialog>()

        override fun doInBackground(vararg voids: Void): Void? {
            for (groupDialog in groupDialogsToDelete) {
                try {
                    errorOccurs = false
                    leaveChatDialog(groupDialog)

                    val currentUser = getCurrentUser()
                    val qbRequestBuilder = QBDialogRequestBuilder()
                    qbRequestBuilder.removeUsers(currentUser?.id!!)

                    QBRestChatService.updateGroupChatDialog(groupDialog, qbRequestBuilder).perform()

                } catch (e: XMPPException) {
                    errorOccurs = true
                    callback?.onError(QBResponseException(e.message))
                } catch (e: SmackException.NotConnectedException) {
                    errorOccurs = true
                    callback?.onError(QBResponseException(e.message))
                } catch (e: QBResponseException) {
                    errorOccurs = true
                    callback?.onError(QBResponseException(e.message))

                } finally {
                    if (!errorOccurs) {
                        successfulDeletedDialogs.add(groupDialog)
                    }
                }
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            if (callback != null && !errorOccurs) {
                callback.onSuccess(successfulDeletedDialogs, null)
            }
        }
    }


    fun loadUsersByPagedRequestBuilder(
        callback: QBEntityCallback<java.util.ArrayList<QBUser>>,
        requestBuilder: QBPagedRequestBuilder
    ) {
        QBUsers.getUsers(requestBuilder).performAsync(callback)
    }

    fun loadUsersByIds(
        usersIDs: Collection<Int>,
        callback: QBEntityCallback<java.util.ArrayList<QBUser>>
    ) {
        QBUsers.getUsersByIDs(usersIDs, null).performAsync(callback)
    }
}
