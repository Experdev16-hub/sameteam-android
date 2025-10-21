package com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil.setContentView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.databinding.ActivityChatBinding
import com.example.sameteam.fcm.sendPushMessage
import com.example.sameteam.helper.*
import com.example.sameteam.helper.Constants.EXTRA_DIALOG_ID
import com.example.sameteam.helper.Constants.EXTRA_IS_NEW_DIALOG
import com.example.sameteam.helper.Constants.IS_IN_BACKGROUND
import com.example.sameteam.helper.Constants.MAX_MESSAGE_SYMBOLS_LENGTH
import com.example.sameteam.helper.Constants.MAX_OPPONENTS_COUNT
import com.example.sameteam.helper.Constants.SEND_TYPING_STATUS_DELAY
import com.example.sameteam.helper.Constants.TYPING_STATUS_DELAY
import com.example.sameteam.helper.Constants.TYPING_STATUS_INACTIVITY_DELAY
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.AttachmentPreviewAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.ChatAdapter
import com.example.sameteam.quickBlox.PaginationHistoryListener
import com.example.sameteam.quickBlox.QbChatDialogMessageListenerImpl
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.VerboseQbChatConnectionListener
import com.example.sameteam.quickBlox.base.BaseActivity2
import com.example.sameteam.quickBlox.chat.CHAT_HISTORY_ITEMS_PER_PAGE
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.WebRtcSessionManager
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.quickBlox.util.shortToast
import com.google.gson.Gson
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBMessageStatusesManager
import com.quickblox.chat.QBRoster
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.*
import com.quickblox.chat.model.*
import com.quickblox.content.model.QBFile
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.messages.QBPushNotifications
import com.quickblox.messages.model.QBEvent
import com.quickblox.messages.model.QBNotificationType
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import com.quickblox.videochat.webrtc.QBRTCClient
import com.quickblox.videochat.webrtc.QBRTCTypes
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smackx.muc.DiscussionHistory
import org.json.JSONObject
import java.io.File
import java.util.*


class ChatActivity : BaseActivity2(), QBMessageStatusListener,
    DialogsManager.ManagingDialogsCallbacks, PickiTCallbacks {
    private val TAG = "ChatActivity"

    private lateinit var chatAdapter: ChatAdapter
    lateinit var qbChatDialog: QBChatDialog
    lateinit var binding: ActivityChatBinding
    private var isChatLoading = false
    private var currentUser = QBUser()

    private var chatMessageListener: ChatMessageListener = ChatMessageListener()
    private lateinit var qbMessageStatusesManager: QBMessageStatusesManager
    private var dialogsManager: DialogsManager = DialogsManager()
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    private lateinit var imageAttachClickListener: ImageAttachClickListener


    private lateinit var chatConnectionListener: ConnectionListener
    private var unShownMessages: ArrayList<QBChatMessage>? = null
    private lateinit var messagesList: MutableList<QBChatMessage>
    private var skipPagination = 0
    private var checkAdapterInit: Boolean = false
    lateinit var contactsRoster: QBRoster
    val REQUEST_PERMISSION_SETTING = 101
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var activityCameraResultLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var attachmentPreviewAdapter: AttachmentPreviewAdapter

    var callUsersList: ArrayList<QBUser> = ArrayList()
    var callUsersIdList: ArrayList<Int> = ArrayList()
    var attachmentUrl = ""

    var pickiT: PickiT? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        SharedPrefsHelper.delete(IS_IN_BACKGROUND)

        binding = setContentView(this, R.layout.activity_chat)

        if (!ChatHelper.isLogged()) {
            reloginToChat()
        }

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e(TAG, "Finishing $TAG. Current user is null")
            finish()
        }

        val gson = Gson()
        pickiT = PickiT(this, this, this)
        try {
            qbChatDialog =
                gson.fromJson(intent.getStringExtra("chatDialog"), QBChatDialog::class.java)
            Log.e("03/08", Gson().toJson(qbChatDialog))
            contactsRoster = QBChatService.getInstance().roster
            contactsRoster.subscriptionMode = QBRoster.SubscriptionMode.mutual
            contactsRoster.addRosterListener(rosterListener)
            contactsRoster.addSubscriptionListener(subscriptionListener)
            contactsRoster.sendPresence(QBPresence(QBPresence.Type.online))


            if (qbChatDialog.type == QBDialogType.PRIVATE) {

                val tempId = qbChatDialog.occupants.filter { it != currentUser.id }[0]

                if (tempId != null) {
                    val tempUser = QbUsersDbManager.getUserById(tempId)
                    binding.initials.visibility = View.GONE
                    binding.name.text = tempUser?.fullName


                    if (tempUser != null && !tempUser.customData.isNullOrBlank()) {
                        QbUsersDbManager.saveUser(tempUser)
                        callUsersList.clear()
                        callUsersIdList.clear()
                        callUsersList.add(tempUser)
                        callUsersIdList.add(tempUser.id)

                        val newUser = Utils.getUserFromQBUser(tempUser.customData)

                        contactsRoster.subscribe(tempUser.id)
                        val onlineUsers = SharedPrefs.getOnlineUserIds(MyApplication.getInstance())
                        if (onlineUsers.contains(tempUser.id))
                            binding.onlineStatus.visibility = View.VISIBLE
                        else
                            binding.onlineStatus.visibility = View.GONE

                        Glide.with(this)
                            .load(newUser.profile_picture)
                            .error(R.drawable.profile_photo)
                            .placeholder(R.drawable.profile_photo)
                            .circleCrop()
                            .into(binding.profileImage)
                    }
                }
            } else {
                callUsersList.clear()
                callUsersIdList.clear()
                binding.name.text = qbChatDialog.name

                for (id in qbChatDialog.occupants) {
                    if (id != currentUser.id) {

                        contactsRoster.subscribe(id)

                        val tempUser = QbUsersDbManager.getUserById(id)
                        if (tempUser != null) {
                            callUsersList.add(tempUser)
                            callUsersIdList.add(tempUser.id)
                        }
                    }
                }

                if (qbChatDialog.photo.isNullOrBlank()) {
                    binding.initials.visibility = View.VISIBLE
                    binding.profileImage.visibility = View.GONE

                    val textArray = qbChatDialog.name.split(" ").toTypedArray()
                    if (textArray.size < 2) {
                        if (textArray[0].length < 2) binding.initials.text =
                            textArray[0][0].toString().uppercase(Locale.getDefault())
                        else binding.initials.text = textArray[0].substring(0, 2)
                            .uppercase(Locale.getDefault())
                    } else {
                        Log.e("Name Check", Gson().toJson(textArray))
                        if (
                            textArray[0][0] != null && textArray[0][0].toString()
                                .isNotEmpty() && textArray[1][0] != null && textArray[1][0].toString()
                                .isNotEmpty()
                        ) {
                            binding.initials.text =
                                "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
                        } else if (textArray[0][0] != null && textArray[0][0].toString()
                                .isNotEmpty()
                        ) {
                            binding.initials.text =
                                "${textArray[0][0].uppercaseChar()}"
                        }

                    }
                } else {
                    binding.initials.visibility = View.GONE
                    binding.profileImage.visibility = View.VISIBLE

                    Glide.with(this)
                        .load(qbChatDialog.photo)
                        .error(R.drawable.profile_photo)
                        .placeholder(R.drawable.profile_photo)
                        .circleCrop()
                        .into(binding.profileImage)
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(R.string.something_went_wrong)
            finish()
        }


        val adminId = qbChatDialog.userId

        binding.etChatMessage.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        binding.etChatMessage.addTextChangedListener(TextInputWatcher())


        try {
            if (ChatHelper.isLogged()) {
                qbChatDialog.initForChat(QBChatService.getInstance())
            }
        } catch (e: IllegalStateException) {
            Log.d(TAG, "initForChat error. Error message is : " + e.message)
            Log.e(TAG, "Finishing $TAG. Unable to init chat")
            finish()
        }

        qbChatDialog.addMessageListener(chatMessageListener)
        qbChatDialog.addIsTypingListener(TypingStatusListener())


        initViews()
        initMessagesRecyclerView()
        initChatConnectionListener()
        initChat()
        startLoginService()

        binding.ivChatSend.setOnClickListener {
            if (Utils.isConnected(this@ChatActivity)) {
                onSendClicked()
            } else {
                shortToast(this@ChatActivity.getString(R.string.no_internet))
            }
        }

        binding.etChatMessage.setOnEditorActionListener { v, actionId, event ->
            var mHandled = false
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                mHandled = true
                onSendClicked()
            }
            mHandled
        }
        try {
            if (::qbChatDialog.isInitialized && !qbChatDialog.isPrivate && currentUser.id == adminId
            ) {
                binding.btnEdit.visibility = View.VISIBLE
            } else {
                binding.btnEdit.visibility = View.GONE
            }

        } catch (
            e: java.lang.Exception
        ) {
            e.printStackTrace()
        }

        binding.btnEdit.setOnClickListener {
            if (qbChatDialog != null) {
                val intent = Intent(this, CreateTeamActivity::class.java)
                val gson = Gson()
                intent.putExtra("editChatDialog", gson.toJson(qbChatDialog))
                startActivity(intent)
            }
        }

        binding.btnInfo.setOnClickListener {
            val intent = Intent(this, TeamDetailsActivity::class.java)
            val gson = Gson()
            intent.putExtra("chatDialog", gson.toJson(qbChatDialog))
            startActivity(intent)
        }
        binding.leftIcon.setOnClickListener {
            onBackPressed()
        }

        binding.btnAudioCall.setOnClickListener {
            checkAudioPermission()
        }

        binding.btnVideoCall.setOnClickListener {
            checkAudioVideoPermissions()

        }

        binding.ivChatAttachment.setOnClickListener {

            if (Utils.isConnected(this@ChatActivity)) {
                if (attachmentPreviewAdapter.count >= 1) {
                    shortToast(R.string.error_attachment_count)
                } else {
                    onAttachmentClicked()
                }
            } else {
                shortToast(this@ChatActivity.getString(R.string.no_internet))
            }

        }

        binding.profileImage.setOnClickListener {
            gotoCreateTeamActivity()
        }
        binding.initials.setOnClickListener {
            gotoCreateTeamActivity()
        }
        binding.nameLayout.setOnClickListener {
            gotoCreateTeamActivity()
        }

        activityCameraResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    Log.d(TAG, "initActivity: ${result.data!!.getParcelableExtra<Uri>("path")} ")

                    val selectedPhotoUri = result.data!!.getParcelableExtra<Uri>("path")
                    try {
                        Log.d(
                            TAG,
                            "initActivity last segment: ${selectedPhotoUri?.lastPathSegment}"
                        )

                        if (selectedPhotoUri != null) {
                            attachmentPreviewAdapter.add(File(selectedPhotoUri.path!!))
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(
                            "19/01 Exception =-=>",
                            e.message.toString()
                        )
                        showErrorSnackbar(
                            R.string.something_went_wrong,
                            e,
                            View.OnClickListener { onAttachmentClicked() })
                    }
                }
            }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    Log.d(TAG, "initActivity: ${result.data!!.getParcelableExtra<Uri>("path")} ")

                    if (pickiT != null) {
                        pickiT!!.getPath(result?.data?.data, Build.VERSION.SDK_INT);
                    }

                    /* var originalUri: Uri? = null
                     if (Build.VERSION.SDK_INT < 19) {
                         originalUri = result?.data?.data
                     } else {
                         originalUri = result?.data?.data

                         val resInfoList =
                             packageManager.queryIntentActivities(
                                 intent,
                                 PackageManager.MATCH_DEFAULT_ONLY
                             )
                         for (resolveInfo in resInfoList) {
                             val packageName = resolveInfo.activityInfo.packageName
                             grantUriPermission(
                                 packageName,
                                 originalUri,
                                 Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                             )
                         }

                     }

 //                    val uri = result?.data
 //                    Log.e("08/02 uri", "uri!!: ${uri?.data} ")


                     try {

                         var strText = getFileFromUri(this@ChatActivity, originalUri)
                         Log.e("08/02 strText", "strText!!: ${strText} ")

 //                        strText = File(originalUri.toString())
 //                        Log.e("22/01", "strText 3423445!!: ${strText.path} ")
                         var filePath: String? = ""

                         if (strText != null && strText.exists()) {
                             filePath = strText.path
                             Log.e("08/02", "filePath 11111!!: ${filePath} ")
                         } else {
                             filePath = originalUri?.let { FileUtils.getPath(this, it) }
                             Log.e("08/02", "filePath!!: ${filePath} ")
                         }

                         if (Utils.checkSelectValidFile(File(filePath)) == true) {
                             attachmentPreviewAdapter.add(File(filePath))
                         } else {
                             showErrorSnackbar(
                                 R.string.file_not_supported,
                                 null,
                                 View.OnClickListener { onAttachmentClicked() })
                         }

 //                            attachmentPreviewAdapter.add(File("/storage/emulated/0/Download/dummy.pdf"))
 //                        }
                     } catch (e: Exception) {
                         e.printStackTrace()
                         Log.e(
                             "19/01 Exception -=-=-=>",
                             e.message.toString() + "-=-=-=-=>" + e.localizedMessage
                         )
                         showErrorSnackbar(
                             R.string.something_went_wrong,
                             e,
                             View.OnClickListener { onAttachmentClicked() })
                     }*/
                }
            }

    }

    fun getFileFromUri(context: Context, uri: Uri?): File? {
        uri ?: return null
        uri.path ?: return null

        var newUriString = uri.toString()
        newUriString = newUriString.replace(
            "content://com.android.providers.downloads.documents/",
            "content://com.android.providers.media.documents/"
        )
        newUriString = newUriString.replace(
            "/msf%3A", "/image%3A"
        )
        val newUri = Uri.parse(newUriString)

        var realPath = String()
        val databaseUri: Uri
        val selection: String?
        val selectionArgs: Array<String>?
        if (newUri.path?.contains("/document/image:") == true) {
            databaseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            selection = "_id=?"
            selectionArgs = arrayOf(DocumentsContract.getDocumentId(newUri).split(":")[1])
        } else {
            databaseUri = newUri
            selection = null
            selectionArgs = null
        }
        try {
            val column = "_data"
            val projection = arrayOf(column)
            val cursor = context.contentResolver.query(
                databaseUri,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.let {
                if (it.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(column)
                    realPath = cursor.getString(columnIndex)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e("08/02 Exception", e.message ?: "")
            Log.i("GetFileUri Exception:", e.message ?: "")
        }
        val path = realPath.ifEmpty {
            when {
                newUri.path?.contains("/document/raw:") == true -> newUri.path?.replace(
                    "/document/raw:",
                    ""
                )

                newUri.path?.contains("/document/primary:") == true -> newUri.path?.replace(
                    "/document/primary:",
                    "/storage/emulated/0/"
                )

                else -> return null
            }
        }
        return if (path.isNullOrEmpty()) null else File(path)
    }

    private fun onSendClicked() {
        try {
            qbChatDialog.sendStopTypingNotification()
        } catch (e: XMPPException) {
            Log.d(TAG, "onCreate: ${e.printStackTrace()}")
        } catch (e: SmackException.NotConnectedException) {
            Log.d(TAG, "onCreate: ${e.printStackTrace()}")
        }

        val totalAttachmentsCount = attachmentPreviewAdapter.count
        val uploadedAttachments = attachmentPreviewAdapter.uploadedAttachments
        if (uploadedAttachments.isNotEmpty()) {
            if (uploadedAttachments.size == totalAttachmentsCount) {
                for (attachment in uploadedAttachments) {
                    sendChatMessage(null, attachment)
                }
            } else {
                shortToast(R.string.chat_wait_for_attachments_to_upload)
            }
        }

        var text = binding.etChatMessage.text.toString().trim { it <= ' ' }
        if (!TextUtils.isEmpty(text)) {
            if (text.length > MAX_MESSAGE_SYMBOLS_LENGTH) {
                text = text.substring(0, MAX_MESSAGE_SYMBOLS_LENGTH)
            }
            sendChatMessage(text, null)
        }
    }

    private fun gotoCreateTeamActivity() {
        if (::qbChatDialog.isInitialized && !qbChatDialog.isPrivate) {
            val intent = Intent(this, TeamDetailsActivity::class.java)
            val gson = Gson()
            intent.putExtra("chatDialog", gson.toJson(qbChatDialog))
            startActivity(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString(EXTRA_DIALOG_ID, qbChatDialog.dialogId)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        try {
            val dialogId = savedInstanceState.getString(EXTRA_DIALOG_ID)!!
            qbChatDialog = QbDialogHolder.getChatDialogById(dialogId)!!
        } catch (e: Exception) {
            Log.d(TAG, e.message!!)
        }
    }

    override fun onResumeFinished() {
        SharedPrefs.saveGroupName(MyApplication.getInstance(), "")

        if (ChatHelper.isLogged()) {
            if (!this::qbChatDialog.isInitialized) {
                qbChatDialog = intent.getSerializableExtra(EXTRA_DIALOG_ID) as QBChatDialog
            }
            returnToChat()
        } else {
            reloginToChat()
        }
    }

    override fun onPause() {
        super.onPause()
        chatAdapter.removeClickListeners()
        ChatHelper.removeConnectionListener(chatConnectionListener)
        if (::qbMessageStatusesManager.isInitialized)
            qbMessageStatusesManager.removeMessageStatusListener(this)
        SharedPrefsHelper.save(IS_IN_BACKGROUND, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (!isChangingConfigurations && pickiT != null) {
                pickiT!!.deleteTemporaryFile(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        qbChatDialog.removeMessageListrener(chatMessageListener)
        dialogsManager.removeManagingDialogsCallbackListener(this)
        SharedPrefsHelper.delete(IS_IN_BACKGROUND)
    }


    private fun sendChatMessage(text: String?, attachment: QBAttachment?) {
        if (ChatHelper.isLogged()) {
            val chatMessage = QBChatMessage()
            attachment?.let {
                chatMessage.addAttachment(it)

                /**
                 * To make array of images uploaded in group to custom data of group
                 */
                if (qbChatDialog.type != QBDialogType.PRIVATE) {
                    try {
                        val fullImageUrl = QBFile.getPrivateUrlForUID(it.id)
                        val imageUrl = fullImageUrl.substringBefore("?token", "")

                        if (imageUrl.isNotBlank()) {
                            val newCustomData = QBDialogCustomData("QBTeam")

                            if (qbChatDialog.customData != null) {
                                val customData = qbChatDialog.customData

                                val imageUrls: List<String>? = customData.getArray("imageUrls")
                                if (imageUrls != null) {
                                    val newList = ArrayList(imageUrls)
                                    newList.add(0, imageUrl)
                                    newCustomData.putArray("imageUrls", newList)
                                } else {
                                    val newList = arrayListOf(imageUrl)
                                    newCustomData.putArray("imageUrls", newList)
                                }

                            } else {
                                val newList = arrayListOf<String>(imageUrl)
                                newCustomData.putArray("imageUrls", newList)
                            }

                            ChatHelper.updateTeamImages(
                                qbChatDialog,
                                newCustomData,
                                object : QBEntityCallback<QBChatDialog> {
                                    override fun onSuccess(p0: QBChatDialog?, p1: Bundle?) {
                                        Log.d(TAG, "onSuccess: Image Added")
                                    }

                                    override fun onError(p0: QBResponseException?) {
                                        p0?.printStackTrace()
                                    }

                                })
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } ?: run {
                chatMessage.body = text
            }

            chatMessage.setSaveToHistory(true)
            chatMessage.dateSent = System.currentTimeMillis() / 1000
            chatMessage.isMarkable = true

            if (qbChatDialog.type != QBDialogType.PRIVATE && !qbChatDialog.isJoined) {
                qbChatDialog.join(DiscussionHistory())
                shortToast(R.string.chat_still_joining)
                return
            }
            try {
                Log.d(TAG, "Sending Message with ID: " + chatMessage.id)
                qbChatDialog.sendMessage(chatMessage)

                if (qbChatDialog.type == QBDialogType.PRIVATE) {
                    showMessage(chatMessage)
                }

                attachment?.let {
                    attachmentPreviewAdapter.remove(it)
                } ?: run {
                    binding.etChatMessage.setText("")
                }

                var groupName = ""
                if (qbChatDialog.type != QBDialogType.PRIVATE) {
                    groupName = qbChatDialog.name
                    sendNotification(chatMessage, true, groupName)
                } else sendNotification(chatMessage, false, groupName)

            } catch (e: SmackException.NotConnectedException) {
                Log.w(TAG, "Caught Exception $e")
                shortToast(R.string.chat_error_send_message)
            }
        } else {
            binding.progressBar.visibility = View.VISIBLE
            Log.d(TAG, "Relogin to Chat")
            ChatHelper.loginToChat(currentUser,
                object : QBEntityCallback<Void> {
                    override fun onSuccess(p0: Void?, p1: Bundle?) {
                        Log.d(TAG, "Relogin Successful")
                        sendChatMessage(text, attachment)
                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onError(e: QBResponseException) {
                        Log.d(TAG, "Re-login Error: " + e.message)
                        shortToast(R.string.chat_send_message_error)
                        binding.progressBar.visibility = View.GONE
                    }
                })
        }
    }

    fun showMessage(message: QBChatMessage) {
        Log.e(
            "19/01 message -=-=-=-=>",
            Gson().toJson(message)
        )
        if (isAdapterConnected()) {
            chatAdapter.addMessage(message)
            scrollMessageListDown()
        } else {
            delayShowMessage(message)
        }
    }

    private fun isAdapterConnected(): Boolean {
        return checkAdapterInit
    }

    private fun delayShowMessage(message: QBChatMessage) {
        if (unShownMessages == null) {
            unShownMessages = ArrayList()
        }
        unShownMessages!!.add(message)
    }

    private fun reloginToChat() {
    if (isChatLoading) return
    
    isChatLoading = true
    binding.progressBar.visibility = View.VISIBLE

    ChatHelper.loginToChat(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<Void> {
        override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
            isChatLoading = false
            returnToChat()
            binding.progressBar.visibility = View.GONE
        }

        override fun onError(e: QBResponseException?) {
            isChatLoading = false
            binding.progressBar.visibility = View.GONE
            shortToast(getString(R.string.reconnect_failed))
        }
    })
    }
    private fun returnToChat() {
        qbChatDialog.initForChat(QBChatService.getInstance())

        contactsRoster = QBChatService.getInstance().roster
        contactsRoster.subscriptionMode = QBRoster.SubscriptionMode.mutual
        contactsRoster.addRosterListener(rosterListener)
        contactsRoster.addSubscriptionListener(subscriptionListener)
        contactsRoster.sendPresence(QBPresence(QBPresence.Type.online))

        val entries = contactsRoster.entries
        Log.d(TAG, "returnToChat: ${entries.size}")


        if (!qbChatDialog.isJoined) {
            try {
                qbChatDialog.join(DiscussionHistory())
            } catch (e: Exception) {
                Log.e(TAG, "Join Dialog Exception: " + e.message)
                longToast(R.string.error_joining_chat)
            }
        }

        // Loading unread messages received in background
        if (SharedPrefsHelper.get(IS_IN_BACKGROUND, false)) {
            binding.progressBar.visibility = View.VISIBLE
            skipPagination = 0
            checkAdapterInit = false
            loadChatHistory()
        }

        returnListeners()
    }

    private fun returnListeners() {
        if (qbChatDialog.isTypingListeners.isEmpty()) {
            qbChatDialog.addIsTypingListener(TypingStatusListener())
        }

        dialogsManager.addManagingDialogsCallbackListener(this)
        try {
            systemMessagesManager = QBChatService.getInstance().systemMessagesManager
            systemMessagesManager.addSystemMessageListener(systemMessagesListener)
            qbMessageStatusesManager = QBChatService.getInstance().messageStatusesManager
            qbMessageStatusesManager.addMessageStatusListener(this)
        } catch (e: Exception) {
            e.message?.let { Log.d(TAG, it) }
            showErrorSnackbar(R.string.error_getting_chat_service, e) { returnListeners() }
        }
        chatAdapter.setAttachImageClickListener(imageAttachClickListener)
//        chatAdapter.setAttachVideoClickListener(videoAttachClickListener)
//        chatAdapter.setAttachFileClickListener(fileAttachClickListener)
//        chatAdapter.setMessageLongClickListener(messageLongClickListener)
        ChatHelper.addConnectionListener(chatConnectionListener)
    }

    private fun loadChatHistory() {
        ChatHelper.loadChatHistory(qbChatDialog, skipPagination, object :
            QBEntityCallback<ArrayList<QBChatMessage>> {
            override fun onSuccess(messages: ArrayList<QBChatMessage>, args: Bundle?) {
                // The newest messages should be in the end of list,
                // so we need to reverse list to show messages in the right order
                messages.reverse()
                if (checkAdapterInit) {
                    chatAdapter.addMessages(messages)
                } else {
                    checkAdapterInit = true
                    chatAdapter.setMessages(messages)
                    addDelayedMessagesToAdapter()
                }
                if (skipPagination == 0) {
                    scrollMessageListDown()
                }
                skipPagination += CHAT_HISTORY_ITEMS_PER_PAGE
                binding.progressBar.visibility = View.GONE
            }

            override fun onError(e: QBResponseException) {
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "onError: loadChatHistory")
                longToast(R.string.connection_error)
            }
        })
    }

    private fun addDelayedMessagesToAdapter() {
        unShownMessages?.let {
            if (it.isNotEmpty()) {
                val chatList = chatAdapter.getMessages()
                for (message in it) {
                    if (!chatList.contains(message)) {
                        chatAdapter.addMessage(message)
                    }
                }
            }
        }
    }

    private fun scrollMessageListDown() {
        binding.rvChatMessages.scrollToPosition(messagesList.size - 1)
    }

    private fun initMessagesRecyclerView() {

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.rvChatMessages.layoutManager = layoutManager

        messagesList = ArrayList()
        chatAdapter = ChatAdapter(this, qbChatDialog, messagesList)
        chatAdapter.setPaginationHistoryListener(PaginationListener())
        binding.rvChatMessages.addItemDecoration(StickyRecyclerHeadersDecoration(chatAdapter))

        binding.rvChatMessages.adapter = chatAdapter
        imageAttachClickListener = ImageAttachClickListener()
//        videoAttachClickListener = VideoAttachClickListener()
//        fileAttachClickListener = FileAttachClickListener()
//        messageLongClickListener = MessageLongClickListenerImpl()
    }

    private fun initViews() {

        attachmentPreviewAdapter = AttachmentPreviewAdapter(
            this,
            object : AttachmentPreviewAdapter.AttachmentCountChangedListener {
                override fun onAttachmentCountChanged(count: Int) {
                    val visiblePreview = when (count) {
                        0 -> View.GONE
                        else -> View.VISIBLE
                    }
                    binding.llAttachmentPreviewContainer.visibility = visiblePreview
                }
            },
            object : AttachmentPreviewAdapter.AttachmentUploadErrorListener {
                override fun onAttachmentUploadError(e: QBResponseException) {
                    showErrorSnackbar(0, e, View.OnClickListener { v ->
                        onAttachmentClicked()
                    })
                }
            })

        binding.adapterAttachmentPreview.setAdapter(attachmentPreviewAdapter)
    }

    private fun initChat() {
        when (qbChatDialog.type) {
            QBDialogType.GROUP,
//            QBDialogType.PUBLIC_GROUP -> joinGroupChat()
            QBDialogType.PRIVATE -> loadDialogUsers()

            else -> {
                shortToast(R.string.something_went_wrong)
//                shortToast(String.format("%s %s", getString(R.string.chat_unsupported_type), qbChatDialog.type.name))
                finish()
            }
        }
    }

    private fun initChatConnectionListener() {
        val rootView: View = binding.rvChatMessages
        chatConnectionListener = object : VerboseQbChatConnectionListener(rootView) {
            override fun reconnectionSuccessful() {
                super.reconnectionSuccessful()
                skipPagination = 0
                if (qbChatDialog.type == QBDialogType.GROUP || qbChatDialog.type == QBDialogType.PUBLIC_GROUP) {
                    checkAdapterInit = false
//                     Join active room if we're in Group Chat
                    runOnUiThread {
                        joinGroupChat()
                    }

                }
            }
        }
    }

    private fun loadDialogUsers() {
        ChatHelper.getUsersFromDialog(qbChatDialog, object : QBEntityCallback<ArrayList<QBUser>> {
            override fun onSuccess(users: ArrayList<QBUser>, bundle: Bundle?) {
                loadChatHistory()
            }

            override fun onError(e: QBResponseException) {
                showErrorSnackbar(R.string.chat_load_users_error, e) { loadDialogUsers() }
            }
        })
    }

    private fun joinGroupChat() {
        binding.progressBar.visibility = View.VISIBLE
        ChatHelper.join(qbChatDialog, object : QBEntityCallback<Void> {
            override fun onSuccess(result: Void?, b: Bundle?) {
                Log.d(TAG, "Joined to Dialog Successful")
                notifyUsersAboutCreatingDialog()
                binding.progressBar.visibility = View.GONE
                loadDialogUsers()
            }

            override fun onError(e: QBResponseException) {
                Log.d(TAG, "Joining Dialog Error: " + e.message)
                binding.progressBar.visibility = View.GONE
                longToast(R.string.connection_error)
            }
        })
    }

    private fun notifyUsersAboutCreatingDialog() {
        if (intent.getBooleanExtra(EXTRA_IS_NEW_DIALOG, false)) {
            dialogsManager.sendMessageCreatedDialog(qbChatDialog)
            intent.removeExtra(EXTRA_IS_NEW_DIALOG)
        }
    }

    inner class PaginationListener : PaginationHistoryListener {
        override fun downloadMore() {
            Log.w(TAG, "Download More")
            loadChatHistory()
        }
    }

    override fun processMessageDelivered(p0: String?, p1: String?, p2: Int?) {
        if (qbChatDialog.dialogId == p1 && p2 != null && p0 != null) {
            chatAdapter.updateStatusDelivered(p0, p2)
        }
    }

    override fun processMessageRead(p0: String?, p1: String?, p2: Int?) {
        if (qbChatDialog.dialogId == p1 && p2 != null && p0 != null) {
            chatAdapter.updateStatusDelivered(p0, p2)
        }
    }

    private inner class ChatMessageListener : QbChatDialogMessageListenerImpl() {
        override fun processMessage(s: String, qbChatMessage: QBChatMessage, integer: Int?) {
            Log.d(TAG, "Processing Received Message: " + qbChatMessage.body)
            showMessage(qbChatMessage)
        }
    }

    private inner class SystemMessagesListener : QBSystemMessageListener {
        override fun processMessage(qbChatMessage: QBChatMessage) {
            Log.d(TAG, "System Message Received: " + qbChatMessage.id)
            dialogsManager.onSystemMessageReceived(qbChatMessage)
        }

        override fun processError(e: QBChatException?, qbChatMessage: QBChatMessage?) {
            Log.d(
                TAG,
                "System Messages Error: " + e?.message + "With MessageID: " + qbChatMessage?.id
            )
        }
    }

    private val rosterListener: QBRosterListener = object : QBRosterListener {
        override fun entriesDeleted(userIds: Collection<Int>) {

        }

        override fun entriesAdded(userIds: Collection<Int>) {
            Log.d(TAG, "entriesAdded: ${userIds.size}")
        }

        override fun entriesUpdated(userIds: Collection<Int>) {
            Log.d(TAG, "entriesUpdated: ${userIds.size}")
        }

        override fun presenceChanged(presence: QBPresence) {
            Log.d(TAG, "presenceChanged: Called")
            // if a user uses several devices, you need to do additional check for presence
            val qbPresence = contactsRoster.getPresence(presence.userId)

            if (qbPresence.type == QBPresence.Type.online) {
                Log.d(TAG, "presenceChanged: Online")
                SharedPrefs.saveOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
                if (callUsersIdList.contains(presence.userId) && qbChatDialog.type == QBDialogType.PRIVATE) {
                    binding.onlineStatus.visibility = View.VISIBLE
                }

            } else {
                Log.d(TAG, "presenceChanged: Offline")
                SharedPrefs.removeOnlineUserId(MyApplication.getInstance(), qbPresence.userId)
                binding.onlineStatus.visibility = View.GONE
            }
        }
    }

    var subscriptionListener = QBSubscriptionListener {
        try {
            contactsRoster.confirmSubscription(it)
        } catch (e: SmackException.NotConnectedException) {
            Log.d(TAG, "Error : ${e.message}")
        } catch (e: SmackException.NotLoggedInException) {
            Log.d(TAG, "Error : ${e.message}")

        } catch (e: SmackException.NoResponseException) {
            Log.d(TAG, "Error : ${e.message}")

        } catch (e: XMPPException) {
            Log.d(TAG, "Error : ${e.message}")

        }
    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {
    }

    override fun onDialogUpdated(chatDialog: String) {
    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {
    }

    private inner class TypingStatusListener : QBChatDialogTypingListener {

        private var currentTypingUserNames = ArrayList<String>()
        private val usersTimerMap = HashMap<Int, Timer>()

        override fun processUserIsTyping(dialogID: String?, userID: Int?) {
            val currentUserID = currentUser.id
            if (dialogID != null && dialogID == qbChatDialog.dialogId && userID != null && userID != currentUserID) {
                Log.d(TAG, "User $userID is typing")
                updateTypingInactivityTimer(dialogID, userID)
                val user = QbUsersDbManager.getUserById(userID)
                if (user != null && user.fullName != null) {
                    Log.d(TAG, "User $userID is in UsersHolder")
                    addUserToTypingList(user)
                } else {
                    Log.d(TAG, "User $userID not in UsersHolder")
                    QBUsers.getUser(userID).performAsync(object : QBEntityCallback<QBUser> {
                        override fun onSuccess(qbUser: QBUser?, bundle: Bundle?) {
                            qbUser?.let {
                                Log.d(TAG, "User " + qbUser.id + " Loaded from Server")
                                QbUsersDbManager.saveUser(qbUser)
                                addUserToTypingList(qbUser)
                            }
                        }

                        override fun onError(e: QBResponseException?) {
                            Log.d(TAG, "Loading User Error: " + e?.message)
                        }
                    })
                }
            }
        }

        private fun addUserToTypingList(user: QBUser) {
            val userName = if (TextUtils.isEmpty(user.fullName)) user.login else user.fullName
            if (!TextUtils.isEmpty(userName) && !currentTypingUserNames.contains(userName) && usersTimerMap.containsKey(
                    user.id
                )
            ) {
                currentTypingUserNames.add(userName)
            }
            binding.tvTypingStatus.text = makeStringFromNames()
            binding.tvTypingStatus.visibility = View.VISIBLE
        }

        override fun processUserStopTyping(dialogID: String?, userID: Int?) {
            val currentUserID = currentUser.id
            if (dialogID != null && dialogID == qbChatDialog.dialogId && userID != null && userID != currentUserID) {
                Log.d(TAG, "User $userID stopped typing")
                stopInactivityTimer(userID)
                val user = QbUsersDbManager.getUserById(userID)
                if (user != null) {
                    removeUserFromTypingList(user)
                }
            }
        }

        private fun removeUserFromTypingList(user: QBUser) {
            val userName = user.fullName
            userName?.let {
                if (currentTypingUserNames.contains(userName)) {
                    currentTypingUserNames.remove(userName)
                }
            }
            binding.tvTypingStatus.text = makeStringFromNames()
            if (makeStringFromNames().isEmpty()) {
                binding.tvTypingStatus.visibility = View.GONE
            }
        }

        private fun updateTypingInactivityTimer(dialogID: String, userID: Int) {
            stopInactivityTimer(userID)
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    Log.d(
                        "Typing Status",
                        "User with ID $userID Did not refresh typing status. Processing stop typing"
                    )
                    runOnUiThread {
                        processUserStopTyping(dialogID, userID)
                    }
                }
            }, TYPING_STATUS_INACTIVITY_DELAY)
            usersTimerMap.put(userID, timer)
        }

        private fun stopInactivityTimer(userID: Int?) {
            if (usersTimerMap.get(userID) != null) {
                try {
                    usersTimerMap.get(userID)!!.cancel()
                } catch (ignored: NullPointerException) {

                } finally {
                    usersTimerMap.remove(userID)
                }
            }
        }

        private fun makeStringFromNames(): String {
            var result = ""
            val usersCount = currentTypingUserNames.size
            if (usersCount == 1) {
                val firstUser = currentTypingUserNames.get(0)

                if (firstUser.length <= 20) {
                    result = firstUser + " " + getString(R.string.typing_postfix_singular)
                } else {
                    result = firstUser.subSequence(0, 19).toString() +
                            getString(R.string.typing_ellipsis) +
                            " " + getString(R.string.typing_postfix_singular)
                }
            } else if (usersCount == 2) {
                var firstUser = currentTypingUserNames.get(0)
                var secondUser = currentTypingUserNames.get(1)

                if ((firstUser + secondUser).length > 20) {
                    if (firstUser.length >= 10) {
                        firstUser = firstUser.subSequence(0, 9)
                            .toString() + getString(R.string.typing_ellipsis)
                    }

                    if (secondUser.length >= 10) {
                        secondUser = secondUser.subSequence(0, 9)
                            .toString() + getString(R.string.typing_ellipsis)
                    }
                }
                result =
                    firstUser + " and " + secondUser + " " + getString(R.string.typing_postfix_plural)

            } else if (usersCount > 2) {
                var firstUser = currentTypingUserNames.get(0)
                var secondUser = currentTypingUserNames.get(1)
                val thirdUser = currentTypingUserNames.get(2)

                if ((firstUser + secondUser + thirdUser).length <= 20) {
                    result =
                        firstUser + ", " + secondUser + " and " + thirdUser + " " + getString(R.string.typing_postfix_plural)
                } else if ((firstUser + secondUser).length <= 20) {
                    result =
                        firstUser + ", " + secondUser + " and " + (currentTypingUserNames.size - 2).toString() + " more " + getString(
                            R.string.typing_postfix_plural
                        )
                } else {
                    if (firstUser.length >= 10) {
                        firstUser = firstUser.subSequence(0, 9)
                            .toString() + getString(R.string.typing_ellipsis)
                    }
                    if (secondUser.length >= 10) {
                        secondUser = secondUser.subSequence(0, 9)
                            .toString() + getString(R.string.typing_ellipsis)
                    }
                    result = firstUser + ", " + secondUser +
                            " and " + (currentTypingUserNames.size - 2).toString() + " more " + getString(
                        R.string.typing_postfix_plural
                    )
                }
            }
            return result
        }
    }

    private inner class TextInputWatcher : TextWatcher {

        private var timer = Timer()
        private var lastSendTime: Long = 0L

        override fun beforeTextChanged(
            charSequence: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {

        }

        override fun onTextChanged(
            charSequence: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            if (SystemClock.uptimeMillis() - lastSendTime > SEND_TYPING_STATUS_DELAY) {
                lastSendTime = SystemClock.uptimeMillis()
                try {
                    qbChatDialog.sendIsTypingNotification()
                } catch (e: XMPPException) {
                    Log.d(TAG, e.message!!)
                } catch (e: SmackException.NotConnectedException) {
                    Log.d(TAG, e.message!!)
                }

            }
        }

        override fun afterTextChanged(s: Editable?) {
            timer.cancel()
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    try {
                        qbChatDialog.sendStopTypingNotification()
                    } catch (e: XMPPException) {
                        Log.d(TAG, e.message!!)
                    } catch (e: SmackException.NotConnectedException) {
                        Log.d(TAG, e.message!!)
                    }
                }
            }, TYPING_STATUS_DELAY)
        }
    }

    fun onAttachmentClicked() {
        Dexter.withContext(this)
            .withPermissions(Utils.getPermissionAsPerAndroidVersion())
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        showImagePickerOptions()
                    } else {

                        if (!Utils.checkPermissions(
                                this@ChatActivity,
                                Utils.getPermissionAsPerAndroidVersion()
                            )
                        ) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts(
                                "package",
                                this@ChatActivity.packageName,
                                null
                            )
                            intent.data = uri
                            activityResultLauncher?.launch(intent)
                        } else {
                            showImagePickerOptions()
                        }

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(
            this,
            object : ImagePickerActivity.PickerOptionListener {
                override fun onTakeVideoSelected() {

                }

                override fun onTakeCameraSelected() {
                    launchCameraIntent()
                }

                override fun onChooseGallerySelected() {
                    launchGalleryIntent()
                }
            },
            4
        )
    }

    private fun launchCameraIntent() {
        val intent = Intent(this, ImagePickerActivity::class.java)
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_IMAGE_CAPTURE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000)


        activityCameraResultLauncher?.launch(intent)

    }

    private fun launchGalleryIntent() {
        /* val intent = Intent(this, ImagePickerActivity::class.java)
         intent.putExtra(
             ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
             ImagePickerActivity.REQUEST_GALLERY_IMAGE
         )

         // setting aspect ratio
         intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
         intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
         intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

         activityResultLauncher?.launch(intent)*/


//        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
//        chooseFile.type = "*/*"
//        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
//        activityResultLauncher?.launch(chooseFile)


        val mimeTypes = arrayOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/comma-separated-values"
        )

//        if (Build.VERSION.SDK_INT < 19) {
//            val intent = Intent()
//            intent.type = "*/*"
//            intent.action = Intent.ACTION_GET_CONTENT
//            activityResultLauncher?.launch(intent)
//        } else {
//            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//            intent.addCategory(Intent.CATEGORY_OPENABLE)
//            intent.type = "*/*"
//            activityResultLauncher?.launch(intent)
//        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            if (mimeTypes.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
        } else {
            var mimeTypesStr = ""
            for (mimeType in mimeTypes) {
                mimeTypesStr += "$mimeType|"
            }
            intent.type = mimeTypesStr.substring(0, mimeTypesStr.length - 1)
        }
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION);
        /*  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          val resInfoList =
              packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
          for (resolveInfo in resInfoList) {
              val packageName = resolveInfo.activityInfo.packageName
              grantUriPermission(
                  packageName,
                  mediaUri,
                  Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
              )
          }*/
        activityResultLauncher?.launch(intent)
    }

    private inner class ImageAttachClickListener : ChatAdapter.AttachClickListener {
        override fun onAttachmentClicked(itemViewType: Int?, view: View, attachment: QBAttachment) {
            Log.e("19/01 itemViewType -> ", itemViewType.toString())
            Log.e("19/01 attachment -> ", attachment.type)
            Log.e("19/01 attachment id -> ", attachment.id)
            Log.e("19/01 attach name -> ", attachment.name)
            val url = QBFile.getPrivateUrlForUID(attachment.id)
            Log.e("19/01 url -=-=-=-=>", url)
            val intent = Intent(this@ChatActivity, ShowImageActivity::class.java)
            intent.putExtra("imageUrl", url)
            intent.putExtra("fileName", attachment.name)
            startActivity(intent)


            /* startActivity(

                 // Opening pdf from assets folder

                 PdfViewerActivity.launchPdfFromUrl(
                     //PdfViewerActivity.Companion.launchPdfFromUrl(..   :: incase of JAVA
                     this@ChatActivity,
                     url,                                // PDF URL in String format
                     "Pdf title/name ",                        // PDF Name/Title in String format
                     "pdf directory to save",                  // If nothing specific, Put "" it will save to Downloads
                     enableDownload = false,
                     // This param is true by defualt.
                 )
             );*/

            /*  val intent = Intent()
              intent.setDataAndType(Uri.parse(url), "application/pdf")
              startActivity(intent)*/

        }
    }


    private fun checkIsLoggedInChat(): Boolean {
        if (!QBChatService.getInstance().isLoggedIn) {
            startLoginService()
            shortToast(R.string.login_chat_retry)
            return false
        }
        return true
    }

    private fun startLoginService() {
        if (SharedPrefsHelper.hasQbUser()) {
            LoginService.start(this, SharedPrefsHelper.getQbUser()!!)
        }
    }

    private fun checkAudioPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (checkIsLoggedInChat()) {
                        startCall(false)
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(
                        "package",
                        packageName,
                        null
                    )
                    intent.data = uri
                    activityResultLauncher?.launch(intent)
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()
    }

    private fun checkAudioVideoPermissions() {
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0?.areAllPermissionsGranted() == true) {
                        if (checkIsLoggedInChat()) {
                            startCall(true)
                        }
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(
                            "package",
                            this@ChatActivity.packageName,
                            null
                        )
                        intent.data = uri
                        activityResultLauncher?.launch(intent)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()
    }

    private fun startCall(isVideoCall: Boolean) {
        val usersCount = callUsersList.size
        if (usersCount > MAX_OPPONENTS_COUNT) {
            longToast(
                String.format(
                    getString(R.string.error_max_opponents_count),
                    MAX_OPPONENTS_COUNT
                )
            )
            return
        }
        val opponentsList = getIdsSelectedOpponents(callUsersList)
        val conferenceType = if (isVideoCall) {
            QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO
        } else {
            QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO
        }
        val qbrtcClient = QBRTCClient.getInstance(applicationContext)
        val newQbRtcSession =
            qbrtcClient.createNewSessionWithOpponents(opponentsList, conferenceType)
        WebRtcSessionManager.setCurrentSession(newQbRtcSession)

        // Make Users FullName Strings and ID's list for iOS VOIP push
        val newSessionID = newQbRtcSession.sessionID
        val opponentsIDsList = java.util.ArrayList<String>()
        val opponentsNamesList = java.util.ArrayList<String>()
        val usersInCall: ArrayList<QBUser> = ArrayList(callUsersList)

        // the Caller in exactly first position is needed regarding to iOS 13 functionality
        usersInCall.add(0, currentUser)

        for (user in usersInCall) {
            val userId = user.id!!.toString()
            var userName = ""
            if (TextUtils.isEmpty(user.fullName)) {
                userName = user.login
            } else {
                userName = user.fullName
            }

            opponentsIDsList.add(userId)
            opponentsNamesList.add(userName)
        }

        val opponentsIDsString = TextUtils.join(",", opponentsIDsList)
        val opponentNamesString = TextUtils.join(",", opponentsNamesList)

        Log.d(
            TAG,
            "New Session with ID: $newSessionID\n Users in Call: \n$opponentsIDsString\n$opponentNamesString"
        )

        var groupName = ""
        if (qbChatDialog.type != QBDialogType.PRIVATE)
            groupName = qbChatDialog.name

        sendPushMessage(
            opponentsList,
            currentUser.fullName,
            newSessionID,
            opponentsIDsString,
            opponentNamesString,
            isVideoCall,
            groupName
        )
        CallActivity.start(this, false, groupName)
    }

    private fun getIdsSelectedOpponents(selectedUsers: Collection<QBUser>): ArrayList<Int> {
        val opponentsIds = ArrayList<Int>()
        if (!selectedUsers.isEmpty()) {
            for (qbUser in selectedUsers) {
                opponentsIds.add(qbUser.id)
            }
        }
        return opponentsIds
    }

    /**
     * Send Chat notification
     */
    private fun sendNotification(chatMessage: QBChatMessage, isGroup: Boolean, groupName: String?) {
        val qbEvent = QBEvent()
        qbEvent.notificationType = QBNotificationType.PUSH
        qbEvent.environment = Constants.QBEnvironment
        val payload = JSONObject()
        val aps = JSONObject()
        val alert = JSONObject()

        if (isGroup)
            payload.put("notification_type", "GroupChat")
        else
            payload.put("notification_type", "Chat")

        alert.put("title", currentUser.fullName)
        alert.put("subtitle", groupName)

        if (chatMessage.body.isNullOrBlank() && !chatMessage.attachments.isNullOrEmpty()) {
            alert.put("body", "Attachment")
        } else if (!chatMessage.body.isNullOrBlank()) {
            alert.put("body", chatMessage.body)
        } else {
            return
        }
        aps.put("alert", alert)
        payload.put("aps", aps)
        qbEvent.message = payload.toString()
        val userIds = StringifyArrayList(getIdsSelectedOpponents(callUsersList))
        qbEvent.userIds = userIds

        Log.d(TAG, "onmessage : Data " + Gson().toJson(qbEvent))

        QBPushNotifications.createEvents(qbEvent)
            .performAsync(object : QBEntityCallback<List<QBEvent>> {
                override fun onSuccess(p0: List<QBEvent>?, p1: Bundle?) {
                    Log.d("FCM", "onSuccess: ")
                }

                override fun onError(p0: QBResponseException?) {
                    Log.d("FCM", "onError: ${p0?.message}")
//                sendNotification(chatMessage, isGroup , groupName)
                }
            })
    }

    override fun PickiTonUriReturned() {
    }

    override fun PickiTonStartListener() {
    }

    override fun PickiTonProgressUpdate(progress: Int) {
    }

    override fun PickiTonCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        Log.e("09/02 path -=-=-=-=>", path.toString())

        try {

            if (Utils.checkSelectValidFile(File(path)) == true) {
                attachmentPreviewAdapter.add(File(path))
            } else {
                showErrorSnackbar(
                    R.string.file_not_supported,
                    null,
                    View.OnClickListener { onAttachmentClicked() })
            }

//                        }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(
                "19/01 Exception -=-=-=>",
                e.message.toString() + "-=-=-=-=>" + e.localizedMessage
            )
            showErrorSnackbar(
                R.string.something_went_wrong,
                e,
                View.OnClickListener { onAttachmentClicked() })
        }

    }

    override fun PickiTonMultipleCompleteListener(
        paths: ArrayList<String>?,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
    }


}
