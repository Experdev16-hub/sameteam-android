package com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.BR
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.ActivityUserDirectoryBinding
import com.example.sameteam.helper.Constants.ORDER_RULE
import com.example.sameteam.helper.Constants.ORDER_VALUE_UPDATED_AT
import com.example.sameteam.helper.Constants.PLAY_SERVICES_REQUEST_CODE
import com.example.sameteam.helper.Constants.USERS_PAGE_SIZE
import com.example.sameteam.quickBlox.QbChatDialogMessageListenerImpl
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.GenericQueryRule
import com.quickblox.core.request.QBPagedRequestBuilder
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.helper.BaseAsyncTask
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils.loadBannerAd
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.ChatListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.TeamListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.bottonSheet.AddContactBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.AllUsersFragVM
import com.example.sameteam.quickBlox.base.BaseActivity2
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.shortToast
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBIncomingMessagesManager
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogMessageListener
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.request.QBRequestGetBuilder
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser

class UserDirectoryActivity : BaseActivity2(), DialogsManager.ManagingDialogsCallbacks,
    ConfirmDialog.ConfirmClickListener {

    private val TAG = "UserDirectoryActivity"

    lateinit var userDirectoryVM: AllUsersFragVM

    lateinit var binding: ActivityUserDirectoryBinding
    lateinit var chatListAdapter: ChatListAdapter

    private var dialogsManager: DialogsManager = DialogsManager()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    lateinit var teamListAdapter: TeamListAdapter

    //    private lateinit var chatConnectionListener: ConnectionListener
    private lateinit var incomingMessagesManager: QBIncomingMessagesManager
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private var allDialogsMessagesListener: QBChatDialogMessageListener =
        AllDialogsMessageListener()

    lateinit var fragment: Fragment

    private lateinit var currentUser: QBUser
    private var currentPage: Int = 0
    private var qbChatDialog: QBChatDialog? = null
    private var hasNextPage: Boolean = true
    private var teamContactsIdList = mutableListOf<Int>()
    var teamContactUserList = ArrayList<LoginResponseModel.User>()
    var globalUserList = ArrayList<LoginResponseModel.User>()
    private var errorCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_directory)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_directory)
        userDirectoryVM = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(AllUsersFragVM::class.java)

        if (!ChatHelper.isLogged()) {
            reloginToChat()
        }

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e(TAG, "Finishing $TAG. Current user is null")
            finish()
        }

        binding.customToolbar.title.text = getString(R.string.teams)
        binding.customToolbar.rightIcon.setImageResource(R.drawable.ic_search)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }
        binding.searchLayout.tvLabelSearch.visibility = View.GONE
        binding.customToolbar.rightIcon.setOnClickListener {
            binding.searchLayout.root.visibility =
                if (binding.searchLayout.root.isVisible) View.GONE else View.VISIBLE
        }

        binding.adView.loadBannerAd()

        userDirectoryVM.getParticipantIdList()

        binding.userRecView.layoutManager = LinearLayoutManager(this)

        userDirectoryVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE

                        userDirectoryVM.getParticipantIdListResponseModel.data?.userId_list?.let {
                            teamContactsIdList = it
                        }

                        loadUsersFromQB()

                    }

                    Constants.NAVIGATE -> {
                        currentPage -= 1
                        userDirectoryVM.getParticipantIdList()
                    }

                    else -> {
                        showMessage(it)
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        binding.createGroup.setOnClickListener {
            val intent = Intent(this, CreateTeamActivity::class.java)
            startActivity(intent)
        }

        binding.addContact.setOnClickListener {
            if (globalUserList.isNotEmpty()) {
                fragment =
                    AddContactBottomSheet(globalUserList, addContactId = { id ->
                        userDirectoryVM.addUserToContacts(listOf(id))

                        if ((fragment as AddContactBottomSheet).isVisible)
                            (fragment as AddContactBottomSheet).dismiss()
                    })

                (fragment as AddContactBottomSheet).show(
                    supportFragmentManager,
                    AddContactBottomSheet::class.java.name
                )
            }

        }

        binding.btnInfo.setOnClickListener {
            val confirmDialog = ConfirmDialog(
                this,
                "Create a team to share tasks, chat, and/or make audio and video calls.",
                "AlreadyCompleted"
            )
            confirmDialog.show(supportFragmentManager, "Confirm")
        }

        /**
         * Edit text change listener for search functionality
         */
        binding.searchLayout.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.searchLayout.txtSearch.text.toString()
                        .isBlank()
                ) {  // if search text is empty, show all users

                    binding.userRecView.adapter = BindingAdapter(
                        layoutId = R.layout.user_list_layout,
                        br = BR.model,
                        list = teamContactUserList,
                        clickListener = { view, i ->
                            userClicked(teamContactUserList[i])
                        }
                    )

                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.text = getString(R.string.search_with_email)

                    binding.txtUserNotFound.visibility =
                        if (teamContactUserList.isEmpty()) View.VISIBLE else View.GONE

                    binding.userRecView.visibility =
                        if (teamContactUserList.isEmpty()) View.GONE else View.VISIBLE

                } else {

                    val string = binding.searchLayout.txtSearch.text.toString()
                    val searchList =
                        if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                            ArrayList(globalUserList.filter {
                                it.email.toString().contains(string, ignoreCase = true)
                            })
                        else
                            ArrayList(teamContactUserList.filter {
                                "${it.first_name} ${it.last_name}".contains(
                                    string, ignoreCase = true
                                )
                            })


                    binding.userRecView.adapter = BindingAdapter(
                        layoutId = R.layout.user_list_layout,
                        br = BR.model,
                        list = searchList,
                        clickListener = { view, i ->
                            userClicked(searchList[i])
                        }
                    )

                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.visibility =
                        if (searchList.isEmpty()) View.VISIBLE else View.GONE

                    binding.txtUserNotFound.text =
                        if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                            R.string.search_with_email
                        )

                    binding.userRecView.visibility =
                        if (searchList.isEmpty()) View.GONE else View.VISIBLE

                }
            }

        })

        binding.searchLayout.txtSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH && v.text.isNotBlank()) {

                /**
                 * Search if firstname or lastname contains matching search string OR email matches search string
                 */

                val string = binding.searchLayout.txtSearch.text.toString()
                val searchList =
                    if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                        ArrayList(globalUserList.filter {
                            it.email.toString().contains(string, ignoreCase = true)
                        })
                    else
                        ArrayList(teamContactUserList.filter {
                            "${it.first_name} ${it.last_name}".contains(
                                string, ignoreCase = true
                            )
                        })

                binding.userRecView.adapter = BindingAdapter(
                    layoutId = R.layout.user_list_layout,
                    br = BR.model,
                    list = searchList,
                    clickListener = { view, i ->
                        userClicked(searchList[i])
                    }
                )

                binding.progressBar.visibility = View.GONE

                binding.txtUserNotFound.visibility =
                    if (searchList.isEmpty()) View.VISIBLE else View.GONE

                binding.txtUserNotFound.text =
                    if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                        R.string.search_with_email
                    )

                binding.userRecView.visibility =
                    if (searchList.isEmpty()) View.GONE else View.VISIBLE

                true
            } else false
        }
    }

    override fun onResumeFinished() {
        if (ChatHelper.isLogged()) {
            checkPlayServicesAvailable()
            registerQbChatListeners()
        } else {
            reloginToChat()
        }
    }

    private fun reloginToChat() {
//        showProgressDialog(R.string.dlg_relogin)
        binding.progressBar.visibility = View.VISIBLE
        if (SharedPrefsHelper.hasQbUser()) {
            ChatHelper.loginToChat(
                SharedPrefsHelper.getQbUser()!!,
                object : QBEntityCallback<Void> {
                    override fun onSuccess(aVoid: Void?, bundle: Bundle?) {
                        Log.d(TAG, "Relogin Successful")
                        checkPlayServicesAvailable()
                        registerQbChatListeners()
                        binding.progressBar.visibility = View.GONE

                    }

                    override fun onError(e: QBResponseException) {
                        Log.d(TAG, "Relogin Failed " + e.message)
//                    hideProgressDialog()
                        binding.progressBar.visibility = View.GONE
                        showErrorSnackbar(
                            R.string.reconnect_failed,
                            e,
                            View.OnClickListener { reloginToChat() })
                    }
                })
        }
    }


    override fun onStop() {
        super.onStop()
        unregisterQbChatListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterQbChatListeners()
    }

    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_REQUEST_CODE)?.show()
            } else {
                Log.e(TAG, "This device is not supported.")
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun loadUsersFromQB() {
        binding.progressBar.visibility = View.VISIBLE
        currentPage += 1

        val rules = ArrayList<GenericQueryRule>()
        rules.add(GenericQueryRule(ORDER_RULE, ORDER_VALUE_UPDATED_AT))

        val requestBuilder = QBPagedRequestBuilder()
        requestBuilder.rules = rules
        requestBuilder.perPage = USERS_PAGE_SIZE
        requestBuilder.page = currentPage

        loadDialogsFromQb(false,false)
        loadGlobalUsers(requestBuilder)
    }

    private fun loadGlobalUsers(requestBuilder: QBPagedRequestBuilder) {

        QBUsers.getUsers(requestBuilder)
            .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
                    errorCount = 0
                    globalUserList = ArrayList()

                    if (usersList.contains(currentUser)) {
                        usersList.remove(currentUser)
                    }

                    QbUsersDbManager.saveAllUsers(usersList, true)

                    val gson = Gson()
                    for (item in usersList) {
                        if (item.id != currentUser.id && !item.tags.isNullOrEmpty()) {

                            val newUser = gson.fromJson(
                                item.customData,
                                LoginResponseModel.User::class.java
                            )

                            newUser.isTeamContact = teamContactsIdList.contains(item.id)

                            globalUserList.add(newUser)
                        }
                    }

                }

                override fun onError(e: QBResponseException) {
                    binding.progressBar.visibility = View.GONE
                    currentPage -= 1
                    errorCount += 1
                    if (errorCount == 2)
                        showMessage(getString(R.string.select_users_get_users_error))
                    else
                        loadUsersFromQB()
                }
            })

    }
    private fun loadDialogsFromQb(silentUpdate: Boolean, clearDialogHolder: Boolean) {
        var hasMoreDialogs = true
        var searchMap = HashMap<String, QBChatDialog>()


        val requestBuilder = QBRequestGetBuilder()
        requestBuilder.limit = 100
//        requestBuilder.skip = if (clearDialogHolder) {
//            0
//        } else {
//            QbDialogHolder.dialogsMap.size
//        }

        searchMap.clear()

        ChatHelper.getDialogs(requestBuilder, object : QBEntityCallback<ArrayList<QBChatDialog>> {
            override fun onSuccess(dialogs: ArrayList<QBChatDialog>, bundle: Bundle?) {
                Log.d(TAG, "onSuccess: ${Gson().toJson(dialogs)}")

                if (dialogs.size < 100) {
                    hasMoreDialogs = false
                }
                if (clearDialogHolder) {
                    QbDialogHolder.clear()
                    hasMoreDialogs = true
                }
                QbDialogHolder.addDialogs(dialogs)

                if (hasMoreDialogs) {
                    loadDialogsFromQb(true, false)
                }
                loadTeamContactUsers()

            }

            override fun onError(e: QBResponseException) {
                e.message?.let { shortToast(it) }

                binding.progressBar.visibility = View.GONE
            }
        })

    }

    private fun loadTeamContactUsers() {

        val dialogs = ArrayList(QbDialogHolder.dialogsMap.values)
        var tempDialog=ArrayList<QBChatDialog>()
        for(item in dialogs)
        if(item.type== QBDialogType.GROUP){
            tempDialog.add(item)
        }
//        if (teamContactsIdList.isNotEmpty()) {
//
//            QBUsers.getUsersByIDs(teamContactsIdList, requestBuilder)
//                .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
//                    override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
//                        errorCount = 0
//                        teamContactUserList = ArrayList()
//
//                        if (usersList.contains(currentUser)) {
//                            usersList.remove(currentUser)
//                        }
//
//                        QbUsersDbManager.saveAllUsers(usersList, true)
//
//                        val gson = Gson()
//                        for (item in usersList) {
//                            if (item.id != currentUser.id && !item.tags.isNullOrEmpty()) {
//
//                                val newUser = gson.fromJson(
//                                    item.customData,
//                                    LoginResponseModel.User::class.java
//                                )
//
//                                teamContactUserList.add(newUser)
//                            }
//                        }
//
//                        binding.userRecView.adapter = BindingAdapter(
//                            layoutId = R.layout.user_list_layout,
//                            br = BR.model,
//                            list = teamContactUserList,
//                            clickListener = { view, i ->
//                                userClicked(teamContactUserList[i])
//                            }
//                        )
//
//                        binding.progressBar.visibility = View.GONE
//
//                        if (teamContactUserList.isEmpty()) {
//                            binding.recViewLayout.visibility = View.GONE
//                            binding.txtUserNotFound.visibility = View.VISIBLE
//                        } else {
//                            binding.recViewLayout.visibility = View.VISIBLE
//                            binding.txtUserNotFound.visibility = View.GONE
//                        }
//                    }
//
//                    override fun onError(e: QBResponseException) {
//                        binding.progressBar.visibility = View.GONE
//                        currentPage -= 1
//                        errorCount += 1
//                        if (errorCount == 2)
//                            showMessage(getString(R.string.select_users_get_users_error))
//                        else
//                            loadUsersFromQB()
//                    }
//                })
//
//        } else {
//
//
//        }
        teamListAdapter = TeamListAdapter(this@UserDirectoryActivity, tempDialog)
        binding.userRecView.adapter = teamListAdapter
        binding.progressBar.visibility = View.GONE
    }


    private fun isPrivateDialogExist(selectedUser: QBUser): Boolean {
        return QbDialogHolder.hasPrivateDialogWithUser(selectedUser)
    }

    fun userClicked(selectedUser: LoginResponseModel.User) {

        val qbUser = QbUsersDbManager.getQBUserByExternalId(selectedUser.id)

        if (qbUser != null) {
            if (isPrivateDialogExist(qbUser)) {
                val existingDialog = QbDialogHolder.getPrivateDialogWithUser(qbUser)
                val intent = Intent(this, ChatActivity::class.java)
                val gson = Gson()
                intent.putExtra("chatDialog", gson.toJson(existingDialog))
                startActivity(intent)
                finish()
            } else {
                val newChatUsers = ArrayList<QBUser>()
                newChatUsers.add(qbUser)
                newChatUsers.add(currentUser)
                createDialog(newChatUsers)
            }
        }
    }

    private fun createDialog(selectedUsers: ArrayList<QBUser>) {
        ChatHelper.createDialogWithSelectedUsers(selectedUsers, "", "", "", false,
            true,
            object : QBEntityCallback<QBChatDialog> {
                override fun onSuccess(dialog: QBChatDialog, args: Bundle?) {
                    Log.d(TAG, "Creating Dialog Successfully")
//                    isProcessingResultInProgress = false
                    registerQbChatListeners()
                    dialogsManager.sendSystemMessageAboutCreatingDialog(
                        systemMessagesManager,
                        dialog
                    )
                    val dialogs = ArrayList<QBChatDialog>()
                    dialogs.add(dialog)

                    QbDialogHolder.addDialogs(dialogs)
//                    DialogJoinerAsyncTask(this@UserDirectoryActivity, dialogs).execute()
                    val intent = Intent(this@UserDirectoryActivity, ChatActivity::class.java)
                    val gson = Gson()
                    intent.putExtra("chatDialog", gson.toJson(dialog))
                    startActivity(intent)
                    finish()
//                    ChatActivity.startForResult(this@DialogsActivity, REQUEST_DIALOG_ID_FOR_UPDATE, dialog, true)
//                    hideProgressDialog()

                }

                override fun onError(error: QBResponseException) {
                    Log.d(TAG, "Creating Dialog Error: " + error.message)
                    hideProgressDialog()
                    showErrorSnackbar(R.string.dialogs_creation_error, error, null)
                }
            }
        )
    }

    private fun registerQbChatListeners() {
//        ChatHelper.addConnectionListener(chatConnectionListener)
        try {
            systemMessagesManager = QBChatService.getInstance().systemMessagesManager
            incomingMessagesManager = QBChatService.getInstance().incomingMessagesManager
        } catch (e: Exception) {
            Log.d(TAG, "Can not get SystemMessagesManager. Need relogin. " + e.message)
            reloginToChat()
            return
        }

        if (incomingMessagesManager == null) {
            reloginToChat()
            return
        }
        systemMessagesManager.addSystemMessageListener(systemMessagesListener)
        incomingMessagesManager.addDialogMessageListener(allDialogsMessagesListener)
        dialogsManager.addManagingDialogsCallbackListener(this)

//        pushBroadcastReceiver = PushBroadcastReceiver()
//        LocalBroadcastManager.getInstance(this).registerReceiver(pushBroadcastReceiver,
//            IntentFilter(ACTION_NEW_FCM_EVENT))
    }

    private inner class SystemMessagesListener : QBSystemMessageListener {
        override fun processMessage(qbChatMessage: QBChatMessage) {
            dialogsManager.onSystemMessageReceived(qbChatMessage)
        }

        override fun processError(e: QBChatException, qbChatMessage: QBChatMessage) {

        }
    }

    private inner class AllDialogsMessageListener : QbChatDialogMessageListenerImpl() {
        override fun processMessage(s: String, qbChatMessage: QBChatMessage, senderID: Int?) {
            Log.d(TAG, "Processing received Message: " + qbChatMessage.body)
            if (senderID != currentUser.id) {
                dialogsManager.onGlobalMessageReceived(s, qbChatMessage)
            }
        }
    }

    private fun unregisterQbChatListeners() {
        if (this::incomingMessagesManager.isInitialized)
            incomingMessagesManager.removeDialogMessageListrener(allDialogsMessagesListener)
        if (this::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        dialogsManager.removeManagingDialogsCallbackListener(this)
    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {

    }

    override fun onDialogUpdated(chatDialog: String) {
    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {
    }

    private class DialogJoinerAsyncTask(
        activity: Activity,
        private val dialogs: ArrayList<QBChatDialog>
    ) : BaseAsyncTask<Void, Void, Void>() {
//        private val activityRef: WeakReference<TestActivity> = WeakReference(activity)

        @Throws(Exception::class)
        override fun performInBackground(vararg params: Void): Void? {
            if (!isCancelled) {
                ChatHelper.join(dialogs)
            }
            return null
        }

        override fun onResult(result: Void?) {

        }

        override fun onException(e: Exception) {
            super.onException(e)
            if (!isCancelled) {
                Log.d("Dialog Joiner Task", "Error: $e")
            }
        }

        override fun onCancelled() {
            super.onCancelled()
            cancel(true)
        }
    }

    override fun onConfirm(place: String) {

    }

}