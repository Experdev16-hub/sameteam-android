package com.example.sameteam

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.databinding.ActivityMainBinding
import com.example.sameteam.helper.Constants.EXTRA_DIALOG_ID
import com.example.sameteam.helper.Constants.EXTRA_IS_NEW_DIALOG
import com.example.sameteam.helper.Constants.IS_IN_BACKGROUND
import com.example.sameteam.helper.Constants.MAX_MESSAGE_SYMBOLS_LENGTH
import com.example.sameteam.helper.Constants.SEND_TYPING_STATUS_DELAY
import com.example.sameteam.helper.Constants.TYPING_STATUS_DELAY
import com.example.sameteam.helper.Constants.TYPING_STATUS_INACTIVITY_DELAY
import com.example.sameteam.helper.ImagePickerActivity
import com.example.sameteam.helper.Utils
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
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.quickBlox.util.shortToast
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBMessageStatusesManager
import com.quickblox.chat.QBRoster
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogTypingListener
import com.quickblox.chat.listeners.QBMessageStatusListener
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBAttachment
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smackx.muc.DiscussionHistory
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 *
 * This class is not used anywhere, this is just use for testing any code.
 *
 */
class MainActivity : BaseActivity2(), QBMessageStatusListener,
    DialogsManager.ManagingDialogsCallbacks {

    private val TAG = "ChatActivity"

    private lateinit var chatAdapter: ChatAdapter
    lateinit var qbChatDialog: QBChatDialog
    lateinit var binding: ActivityMainBinding

    private var currentUser = QBUser()

    private var chatMessageListener: ChatMessageListener = ChatMessageListener()
    private lateinit var qbMessageStatusesManager: QBMessageStatusesManager
    private var dialogsManager: DialogsManager = DialogsManager()
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
//    private lateinit var imageAttachClickListener: ImageAttachClickListener

    private lateinit var chatConnectionListener: ConnectionListener
    private var unShownMessages: ArrayList<QBChatMessage>? = null
    private lateinit var messagesList: MutableList<QBChatMessage>
    private var skipPagination = 0
    private var checkAdapterInit: Boolean = false
    lateinit var contactsRoster: QBRoster
    val REQUEST_PERMISSION_SETTING = 101
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private lateinit var imagePreviewAdapter: AttachmentPreviewAdapter

    var callUsersList: ArrayList<QBUser> = ArrayList()
    var callUsersIdList: ArrayList<Int> = ArrayList()
    var attachmentUrl = ""

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SharedPrefsHelper.delete(IS_IN_BACKGROUND)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

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

        try {
            qbChatDialog =
                gson.fromJson(intent.getStringExtra("chatDialog"), QBChatDialog::class.java)

            val test = qbChatDialog.dialogId

            if (qbChatDialog.type == QBDialogType.PRIVATE) {

                val tempId = qbChatDialog.occupants.filter { it != currentUser.id }[0]

                if (tempId != null) {
                    val tempUser = QbUsersDbManager.getUserById(tempId)
//                    binding.initials.visibility = View.GONE
                    binding.name.text = tempUser?.fullName


                    if (tempUser != null && !tempUser.customData.isNullOrBlank()) {
                        QbUsersDbManager.saveUser(tempUser)
                        callUsersList.clear()
                        callUsersIdList.clear()
                        callUsersList.add(tempUser)
                        callUsersIdList.add(tempUser.id)

//                        val newUser = Utils.getUserFromQBUser(tempUser.customData)

//                        contactsRoster.subscribe(tempUser.id)
//                        val onlineUsers = SharedPrefs.getOnlineUserIds(MyApplication.getInstance())
//                        if(onlineUsers.contains(tempUser.id))
//                            binding.onlineStatus.visibility = View.VISIBLE
//                        else
//                            binding.onlineStatus.visibility = View.GONE
//
//                        Glide.with(this)
//                            .load(newUser.profile_picture)
//                            .error(com.quickblox.chat.R.drawable.profile_photo)
//                            .placeholder(com.quickblox.chat.R.drawable.profile_photo)
//                            .circleCrop()
//                            .into(binding.profileImage)
                    }
                }
            } else {
                callUsersList.clear()
                callUsersIdList.clear()
                binding.name.text = qbChatDialog.name

                for (id in qbChatDialog.occupants) {
                    if (id != currentUser.id) {

                        val tempUser = QbUsersDbManager.getUserById(id)
                        if (tempUser != null) {
                            callUsersList.add(tempUser)
                            callUsersIdList.add(tempUser.id)
                        }
                    }
                }

//                if(qbChatDialog.photo.isNullOrBlank()){
//                    binding.initials.visibility = View.VISIBLE
//                    binding.profileImage.visibility = View.GONE
//
//                    val textArray = qbChatDialog.name.split(" ").toTypedArray()
//                    if(textArray.size < 2){
//                        if(textArray[0].length < 2) binding.initials.text =
//                            textArray[0][0].toString().toUpperCase(Locale.ROOT)
//                        else binding.initials.text = textArray[0].substring(0,2).toUpperCase(
//                            Locale.ROOT
//                        )
//                    }
//                    else{
//                        binding.initials.text = "${textArray[0][0].toUpperCase()}${textArray[1][0].toUpperCase()}"
//                    }
//                }
//                else{
//                    binding.initials.visibility = View.GONE
//                    binding.profileImage.visibility = View.VISIBLE
//
//                    Glide.with(this)
//                        .load(qbChatDialog.photo)
//                        .error(com.quickblox.chat.R.drawable.profile_photo)
//                        .placeholder(com.quickblox.chat.R.drawable.profile_photo)
//                        .circleCrop()
//                        .into(binding.profileImage)
//                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(R.string.something_went_wrong)
            finish()
        }


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

        binding.ivChatSend.setOnClickListener {

            try {
                qbChatDialog.sendStopTypingNotification()
            } catch (e: XMPPException) {
                Log.d(TAG, "onCreate: ${e.printStackTrace()}")
            } catch (e: SmackException.NotConnectedException) {
                Log.d(TAG, "onCreate: ${e.printStackTrace()}")
            }

            val totalAttachmentsCount = imagePreviewAdapter.count
            val uploadedAttachments = imagePreviewAdapter.uploadedAttachments
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

        binding.leftIcon.setOnClickListener {
            onBackPressed()
        }

//        binding.btnAudioCall.setOnClickListener {
//            checkAudioPermission()
//        }
//
//        binding.btnVideoCall.setOnClickListener {
//            checkAudioVideoPermissions()
//
//        }

        binding.ivChatAttachment.setOnClickListener {
            if (imagePreviewAdapter.count >= 1) {
                shortToast(R.string.error_attachment_count)
            } else {
                onAttachmentClicked()
            }
        }

//        binding.profileImage.setOnClickListener {
//            gotoCreateTeamActivity()
//        }
//        binding.initials.setOnClickListener {
//            gotoCreateTeamActivity()
//        }
//        binding.nameLayout.setOnClickListener {
//            gotoCreateTeamActivity()
//        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    Log.d(TAG, "initActivity: ${result.data!!.getParcelableExtra<Uri>("path")} ")

                    val selectedPhotoUri = result.data!!.getParcelableExtra<Uri>("path")
                    try {

                        Log.d(
                            TAG,
                            "initActivity last segment: ${selectedPhotoUri?.lastPathSegment}"
                        )


//                        Glide.with(this)
//                            .load(selectedPhotoUri)
//                            .error(R.drawable.profile_photo)
//                            .placeholder(R.drawable.profile_photo)
//                            .circleCrop()
//                            .into(binding.profilePic)

                        if (selectedPhotoUri != null) {
                            imagePreviewAdapter.add(File(selectedPhotoUri.path!!))
                        }


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
        if (ChatHelper.isLogged()) {
            if (!this::qbChatDialog.isInitialized) {
                val gson = Gson()
                qbChatDialog =
                    gson.fromJson(intent.getStringExtra("chatDialog"), QBChatDialog::class.java)
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
        if (::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        qbChatDialog.removeMessageListrener(chatMessageListener)
        dialogsManager.removeManagingDialogsCallbackListener(this)
        SharedPrefsHelper.delete(IS_IN_BACKGROUND)
    }

    private fun reloginToChat() {
        binding.progressBar.visibility = View.VISIBLE
        ChatHelper.loginToChat(SharedPrefsHelper.getQbUser()!!, object : QBEntityCallback<Void> {
            override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                returnToChat()
                binding.progressBar.visibility = View.GONE
            }

            override fun onError(e: QBResponseException?) {
                binding.progressBar.visibility = View.GONE
                showErrorSnackbar(
                    R.string.reconnect_failed,
                    e,
                    View.OnClickListener { reloginToChat() })
            }
        })
    }

    private fun returnToChat() {
        qbChatDialog.initForChat(QBChatService.getInstance())
        if (!qbChatDialog.isJoined) {
            try {
                qbChatDialog.join(DiscussionHistory())
            } catch (e: Exception) {
                Log.e(TAG, "Join Dialog Exception: " + e.message)
                showErrorSnackbar(
                    R.string.error_joining_chat,
                    e,
                    View.OnClickListener { returnToChat() })
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
            showErrorSnackbar(
                R.string.error_getting_chat_service,
                e,
                View.OnClickListener { returnListeners() })
        }
//        chatAdapter.setAttachImageClickListener(imageAttachClickListener)
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
//        imageAttachClickListener = ImageAttachClickListener()
//        videoAttachClickListener = VideoAttachClickListener()
//        fileAttachClickListener = FileAttachClickListener()
//        messageLongClickListener = MessageLongClickListenerImpl()
    }


    inner class PaginationListener : PaginationHistoryListener {
        override fun downloadMore() {
            Log.w(TAG, "Download More")
            loadChatHistory()
        }
    }

    private fun initViews() {

        imagePreviewAdapter = AttachmentPreviewAdapter(
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

        binding.adapterAttachmentPreview.setAdapter(imagePreviewAdapter)
    }

    fun onAttachmentClicked() {
        Dexter.withContext(this)
            .withPermissions(Utils.getPermissionAsPerAndroidVersion())
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        showImagePickerOptions()
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(
                            "package",
                            this@MainActivity.packageName,
                            null
                        )
                        intent.data = uri
                        activityResultLauncher?.launch(intent)
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
            3
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


        activityResultLauncher?.launch(intent)

    }

    private fun launchGalleryIntent() {
        val intent = Intent(this, ImagePickerActivity::class.java)
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_GALLERY_IMAGE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

        activityResultLauncher?.launch(intent)
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

    private fun sendChatMessage(text: String?, attachment: QBAttachment?) {
        if (ChatHelper.isLogged()) {
            val chatMessage = QBChatMessage()
            attachment?.let {
                chatMessage.addAttachment(it)
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
                    imagePreviewAdapter.remove(it)
                } ?: run {
                    binding.etChatMessage.setText("")
                }
            } catch (e: SmackException.NotConnectedException) {
                Log.w(TAG, e)
                shortToast(R.string.chat_error_send_message)
            }
        } else {
//            showProgressDialog(R.string.dlg_login)
            Log.d(TAG, "Relogin to Chat")
            ChatHelper.loginToChat(currentUser,
                object : QBEntityCallback<Void> {
                    override fun onSuccess(p0: Void?, p1: Bundle?) {
                        Log.d(TAG, "Relogin Successful")
                        sendChatMessage(text, attachment)
                        hideProgressDialog()
                    }

                    override fun onError(e: QBResponseException) {
                        Log.d(TAG, "Relogin Error: " + e.message)
                        hideProgressDialog()
                        shortToast(R.string.chat_send_message_error)
                    }
                })
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

    override fun onDialogCreated(chatDialog: QBChatDialog) {

    }

    override fun onDialogUpdated(chatDialog: String) {
    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {
    }

    private inner class ChatMessageListener : QbChatDialogMessageListenerImpl() {
        override fun processMessage(s: String, qbChatMessage: QBChatMessage, integer: Int?) {
            Log.d(TAG, "Processing Received Message: " + qbChatMessage.body)
            showMessage(qbChatMessage)
        }
    }

    fun showMessage(message: QBChatMessage) {
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
}