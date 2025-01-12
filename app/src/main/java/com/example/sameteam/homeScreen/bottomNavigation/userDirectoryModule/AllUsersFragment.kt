package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.BR
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.LoginActivity
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseFragment
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.FragmentAllUsersBinding
import com.example.sameteam.fcm.sendPushMessage
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CallActivity
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.AllUsersFragVM
import com.example.sameteam.quickBlox.QbChatDialogMessageListenerImpl
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.WebRtcSessionManager
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.quickBlox.util.shortToast
import com.example.sameteam.widget.ConfirmCallDialog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBIncomingMessagesManager
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogMessageListener
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.GenericQueryRule
import com.quickblox.core.request.QBPagedRequestBuilder
import com.quickblox.messages.services.QBPushManager
import com.quickblox.messages.services.SubscribeService
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import com.quickblox.videochat.webrtc.QBRTCClient
import com.quickblox.videochat.webrtc.QBRTCTypes
import kotlinx.coroutines.launch


class AllUsersFragment : BaseFragment<FragmentAllUsersBinding>(),
    DialogsManager.ManagingDialogsCallbacks {

    private val TAG = "AllUsersFragment"

    override fun layoutID() = R.layout.fragment_all_users

    override fun viewModel() = ViewModelProvider(
        this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    ).get(AllUsersFragVM::class.java)

    lateinit var allUsersFragVM: AllUsersFragVM
    lateinit var binding: FragmentAllUsersBinding
    private lateinit var receiver: MyReceiver

    private lateinit var currentUser: QBUser

    private var dialogsManager: DialogsManager = DialogsManager()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    private lateinit var incomingMessagesManager: QBIncomingMessagesManager
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private var allDialogsMessagesListener: QBChatDialogMessageListener =
        AllDialogsMessageListener()

    private var currentPage: Int = 0
    var teamContactUserList = ArrayList<LoginResponseModel.User>()
    var globalUserList = ArrayList<LoginResponseModel.User>()
    private var errorCount: Int = 0
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var callUsersList: ArrayList<QBUser> = ArrayList()
    var callUsersIdList: ArrayList<Int> = ArrayList()
    private var teamContactsIdList = mutableListOf<Int>()

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("Intent", "Action ${intent?.action}")
            lifecycleScope.launch {
                if (intent?.action.equals("allUsersShow")) {
                    binding.searchLayout.root.visibility = View.VISIBLE
                } else if (intent?.action.equals("allUsersHide")) {
                    binding.searchLayout.root.visibility = View.GONE
                }
            }
        }
    }


    override fun initFragment(mBinding: ViewDataBinding) {
        binding = mBinding as FragmentAllUsersBinding
        allUsersFragVM = getViewModel() as AllUsersFragVM

        receiver = MyReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("allUsersShow")
        intentFilter.addAction("allUsersHide")
        context?.registerReceiver(receiver, intentFilter, AppCompatActivity.RECEIVER_EXPORTED)

        if (!ChatHelper.isLogged()) {
            reloginToChat()
        }

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e(TAG, "Finishing $TAG. Current user is null")
            //requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }
        binding.btnSearch.setOnClickListener {
            binding.searchLayout.root.visibility = View.VISIBLE
        }
        binding.searchLayout.tvLabelSearch.visibility = View.VISIBLE

        binding.userRecView.layoutManager = LinearLayoutManager(requireContext())

        allUsersFragVM.getParticipantIdList()

        /**
         * General Observer
         */
        allUsersFragVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE

                        allUsersFragVM.getParticipantIdListResponseModel.data?.userId_list?.let {
                            teamContactsIdList = it
                        }

                        loadUsersFromQB()

                    }
                    Constants.FORCE_LOGOUT -> {
                        unsubscribeFromPushes()
                    }
                    Constants.NAVIGATE -> {
                        currentPage=0
                        binding.searchLayout.root.visibility = View.GONE
                        binding.searchLayout.txtSearch.setText("")
                        Utils.hideKeyboard(requireActivity())
                        allUsersFragVM.getParticipantIdList()
                    }
                    else -> {
                        showMessage(it)
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
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

                    binding.userRecView.adapter =
                        BindingAdapter(layoutId = R.layout.all_users_list_layout,
                            br = BR.model,
                            list = teamContactUserList,
                            clickListener = { view, i ->
                                when (view.id) {
                                    R.id.btnVideoCall -> {
                                        val confirmDialog = ConfirmCallDialog(
                                            requireContext(),
                                            requireActivity().getString(R.string.video_call)
                                           ) { isClicked ->
                                            if (isClicked) {
                                                fetchUser(teamContactUserList[i], true)
                                            }
                                        }
                                            confirmDialog.show(
                                                requireActivity().supportFragmentManager, "Confirm"
                                            )
                                        }
                                        R.id.btnAudioCall -> {
                                            val confirmDialog = ConfirmCallDialog(
                                                requireContext(),
                                                requireActivity().getString(R.string.voice_call)
                                            ) { isClicked ->
                                            if (isClicked) {
                                                fetchUser(teamContactUserList[i], false)
                                            }
                                        }
                                        confirmDialog.show(
                                            requireActivity().supportFragmentManager, "Confirm"
                                        )
                                    }
                                }
                            })
                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.text = getString(R.string.search_with_email)

                    binding.txtUserNotFound.visibility =
                        if (teamContactUserList.isEmpty()) View.VISIBLE else View.GONE

                    binding.userRecView.visibility =
                        if (teamContactUserList.isEmpty()) View.GONE else View.VISIBLE

                } else {

                    val string = binding.searchLayout.txtSearch.text.toString()
                    val searchList =
                        if (Patterns.EMAIL_ADDRESS.matcher(string).matches()) ArrayList(
                            globalUserList.filter {
                                it.email.toString().contains(string, ignoreCase = true)
                            })
                        else ArrayList(teamContactUserList.filter {
                            "${it.first_name} ${it.last_name}".contains(
                                string, ignoreCase = true
                            )
                        })

                    binding.userRecView.adapter =
                        BindingAdapter(layoutId = R.layout.all_users_list_layout,
                            br = BR.model,
                            list = searchList,
                            clickListener = { view, i ->
                                when (view.id) {
                                    R.id.btnVideoCall -> {
                                        val confirmDialog = ConfirmCallDialog(
                                            requireContext(),
                                            requireActivity().getString(R.string.video_call)
                                        ) { isClicked ->
                                            if (isClicked) {
                                                fetchUser(searchList[i], true)
                                            }
                                        }
                                        confirmDialog.show(
                                            requireActivity().supportFragmentManager, "Confirm"
                                        )
                                    }
                                    R.id.btnAudioCall -> {
                                        val confirmDialog = ConfirmCallDialog(
                                            requireContext(),
                                            requireActivity().getString(R.string.voice_call)
                                        ) { isClicked ->
                                            if (isClicked) {
                                                fetchUser(searchList[i], false)
                                            }
                                        }
                                        confirmDialog.show(
                                            requireActivity().supportFragmentManager, "Confirm"
                                        )
                                    }
                                    R.id.btnAdd -> {
                                        searchList[i].id?.let {
                                            allUsersFragVM.addUserToContacts(
                                                listOf(it)
                                            )
                                        }
                                    }
                                }
                            })

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
                val searchList = if (Patterns.EMAIL_ADDRESS.matcher(string).matches()) ArrayList(
                    globalUserList.filter {
                        it.email.toString().contains(string, ignoreCase = true)
                    })
                else ArrayList(teamContactUserList.filter {
                    "${it.first_name} ${it.last_name}".contains(
                        string, ignoreCase = true
                    )
                })

                binding.userRecView.adapter =
                    BindingAdapter(layoutId = R.layout.all_users_list_layout,
                        br = BR.model,
                        list = searchList,
                        clickListener = { view, i ->
                            when (view.id) {
                                R.id.btnVideoCall -> {
                                    val confirmDialog = ConfirmCallDialog(
                                        requireContext(),
                                        requireActivity().getString(R.string.video_call)
                                    ) { isClicked ->
                                        if (isClicked) {
                                            fetchUser(searchList[i], true)
                                        }
                                    }
                                    confirmDialog.show(
                                        requireActivity().supportFragmentManager, "Confirm"
                                    )
                                }
                                R.id.btnAudioCall -> {
                                    val confirmDialog = ConfirmCallDialog(
                                        requireContext(),
                                        requireActivity().getString(R.string.voice_call)
                                    ) { isClicked ->
                                        if (isClicked) {
                                            fetchUser(searchList[i], false)
                                        }
                                    }
                                    confirmDialog.show(
                                        requireActivity().supportFragmentManager, "Confirm"
                                    )
                                }
                                R.id.btnAdd -> {
                                    searchList[i].id?.let {
                                        allUsersFragVM.addUserToContacts(
                                            listOf(it)
                                        )
                                    }
                                }
                            }
                        })

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

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }

    }


    override fun onResume() {
        super.onResume()
        errorCount = 0
        if (ChatHelper.isLogged()) {
            checkPlayServicesAvailable()
            registerQbChatListeners()
        } else {
            reloginToChat()
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterQbChatListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterQbChatListeners()

        try {
            //Register or UnRegister your broadcast receiver here
            context?.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
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

                        activity?.getString(R.string.something_went_wrong)?.let {
                            showMessage(it)
                        }
                    }
                })
        }
    }

    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    requireActivity(), resultCode, Constants.PLAY_SERVICES_REQUEST_CODE
                )?.show()
            } else {
                Log.e(TAG, "This device is not supported.")
                Toast.makeText(requireContext(), "This device is not supported", Toast.LENGTH_SHORT)
                    .show()
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            }
        }
    }

    private fun loadUsersFromQB() {
        binding.progressBar.visibility = View.VISIBLE
        currentPage += 1

        val rules = ArrayList<GenericQueryRule>()
        rules.add(GenericQueryRule(Constants.ORDER_RULE, Constants.ORDER_VALUE_UPDATED_AT))

        val requestBuilder = QBPagedRequestBuilder()
        requestBuilder.rules = rules
        requestBuilder.perPage = Constants.USERS_PAGE_SIZE
        requestBuilder.page = currentPage

        loadTeamContactUsers(requestBuilder)

        loadGlobalUsers(requestBuilder)
    }

    private fun loadGlobalUsers(requestBuilder: QBPagedRequestBuilder) {
        QBUsers.getUsers(requestBuilder).performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
            override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
                errorCount = 0
                globalUserList = ArrayList()

                if (usersList.contains(currentUser)) {
                    usersList.remove(currentUser)
                }
                Log.d(TAG, "QB all User count: ${usersList.count().toString()}")

                QbUsersDbManager.saveAllUsers(usersList, true)

                val gson = Gson()
                for (item in usersList) {
                    if (item.id != currentUser.id && !item.tags.isNullOrEmpty()) {

                        val newUser = gson.fromJson(
                            item.customData, LoginResponseModel.User::class.java
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
                if (errorCount == 2) showMessage(getString(R.string.select_users_get_users_error))
                else loadUsersFromQB()
            }
        })

    }

    private fun loadTeamContactUsers(requestBuilder: QBPagedRequestBuilder) {

        if (teamContactsIdList.isNotEmpty()) {

            QBUsers.getUsersByIDs(teamContactsIdList, requestBuilder)
                .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                    override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
                        errorCount = 0
                        teamContactUserList = ArrayList()

                        if (usersList.contains(currentUser)) {
                            usersList.remove(currentUser)
                        }
                        Log.d(TAG, "QB Team Contact count: ${usersList.count().toString()}")

                        QbUsersDbManager.saveAllUsers(usersList, true)

                        val gson = Gson()
                        for (item in usersList) {
                            if (item.id != currentUser.id && !item.tags.isNullOrEmpty()) {

                                val newUser = gson.fromJson(
                                    item.customData, LoginResponseModel.User::class.java
                                )
                                newUser.isTeamContact = teamContactsIdList.contains(item.id)

                                teamContactUserList.add(newUser)
                            }
                        }

                        binding.userRecView.adapter =
                            BindingAdapter(layoutId = R.layout.all_users_list_layout,
                                br = BR.model,
                                list = teamContactUserList,
                                clickListener = { view, i ->
                                    when (view.id) {
                                        R.id.btnVideoCall -> {
                                            val confirmDialog = ConfirmCallDialog(
                                                requireContext(),
                                                requireActivity().getString(R.string.video_call)
                                            ) { isClicked ->
                                                if (isClicked) {
                                                    fetchUser(teamContactUserList[i], true)
                                                }
                                            }
                                            confirmDialog.show(
                                                requireActivity().supportFragmentManager, "Confirm"
                                            )
                                        }
                                        R.id.btnAudioCall -> {
                                            val confirmDialog = ConfirmCallDialog(
                                                requireContext(),
                                                requireActivity().getString(R.string.voice_call)
                                            ) { isClicked ->
                                                if (isClicked) {
                                                    fetchUser(teamContactUserList[i], false)
                                                }
                                            }
                                            confirmDialog.show(
                                                requireActivity().supportFragmentManager, "Confirm"
                                            )
                                        }
                                    }
                                })

                        binding.progressBar.visibility = View.GONE

                        if (teamContactUserList.isNullOrEmpty()) {
                            binding.recViewLayout.visibility = View.GONE
                            binding.noDataLayout.visibility = View.VISIBLE
                        }
                    }

                    override fun onError(e: QBResponseException) {
                        binding.progressBar.visibility = View.GONE
                        currentPage -= 1
                        errorCount += 1
                        if (errorCount == 2) showMessage(getString(R.string.select_users_get_users_error))
                        else loadUsersFromQB()
                    }
                })

        } else {
            binding.progressBar.visibility = View.GONE

        }

    }


    private fun unsubscribeFromPushes() {
        if (QBPushManager.getInstance().isSubscribedToPushes) {
            QBPushManager.getInstance().addListener(object : QBPushManager.QBSubscribeListener {
                override fun onSubscriptionCreated() {

                }

                override fun onSubscriptionError(e: Exception?, i: Int) {
                    Log.d("Subscription", "SubscriptionError" + e?.localizedMessage)

                    showMessage(getString(R.string.something_went_wrong))
                    e?.printStackTrace()
                }

                override fun onSubscriptionDeleted(success: Boolean) {
                    Log.d(TAG, "Subscription Deleted -> Success: $success")
                    QBPushManager.getInstance().removeListener(this)
                    performLogout()
                }
            })
            SubscribeService.unSubscribeFromPushes(requireContext())
        } else {
            performLogout()
        }
    }

    fun performLogout() {
        ChatHelper.destroy()
        SharedPrefsHelper.clearAllData()
        SharedPrefs.clearAllData(MyApplication.getInstance())
        QbDialogHolder.clear()
        startActivity(LoginActivity::class.java)
        requireActivity().finish()
    }

    private fun registerQbChatListeners() {
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
        if (this::incomingMessagesManager.isInitialized) incomingMessagesManager.removeDialogMessageListrener(
            allDialogsMessagesListener
        )
        if (this::systemMessagesManager.isInitialized) systemMessagesManager.removeSystemMessageListener(
            systemMessagesListener
        )

        dialogsManager.removeManagingDialogsCallbackListener(this)
    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {

    }

    override fun onDialogUpdated(chatDialog: String) {

    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {

    }

    fun fetchUser(user: LoginResponseModel.User?, isVideoCall: Boolean) {
        callUsersList.clear()
        callUsersIdList.clear()
        if (user != null) {
            val tempUser = QbUsersDbManager.getQBUserByExternalId(user.id)
            if (tempUser != null) {
                callUsersList.add(tempUser)
                callUsersIdList.add(tempUser.id)
                if (isVideoCall) checkAudioVideoPermissions()
                else checkAudioPermission()
            }
        }

    }

    private fun checkAudioPermission() {
        Dexter.withContext(requireActivity()).withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (checkIsLoggedInChat()) {
                        startCall(false)
                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(
                        "package", requireActivity().packageName, null
                    )
                    intent.data = uri
                    activityResultLauncher?.launch(intent)
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?, p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()
    }

    private fun checkAudioVideoPermissions() {
        Dexter.withContext(requireActivity())
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
                            "package", requireActivity().packageName, null
                        )
                        intent.data = uri
                        activityResultLauncher?.launch(intent)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?, p1: PermissionToken?
                ) {
                    p1?.continuePermissionRequest()
                }

            }).check()
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
            LoginService.start(MyApplication.getInstance(), SharedPrefsHelper.getQbUser()!!)
        }
    }


    private fun startCall(isVideoCall: Boolean) {

        if (callUsersList.isNullOrEmpty()) {
            longToast("Not enough users to make a call")
            return
        }

        val usersCount = callUsersList.size
        if (usersCount > Constants.MAX_OPPONENTS_COUNT) {
            longToast(
                String.format(
                    getString(R.string.error_max_opponents_count), Constants.MAX_OPPONENTS_COUNT
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
        val qbrtcClient = QBRTCClient.getInstance(MyApplication.getInstance())
        val newQbRtcSession =
            qbrtcClient.createNewSessionWithOpponents(opponentsList, conferenceType)
        WebRtcSessionManager.setCurrentSession(newQbRtcSession)

        // Make Users FullName Strings and ID's list for iOS VOIP push
        val newSessionID = newQbRtcSession.sessionID
        val opponentsIDsList = java.util.ArrayList<String>()
        val opponentsNamesList = java.util.ArrayList<String>()
        val usersInCall: ArrayList<QBUser> = ArrayList(callUsersList)

        // the Caller in exactly first position is needed regarding to iOS 13 functionality
        usersInCall.add(0, currentUser!!)

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


        currentUser?.fullName?.let {
            sendPushMessage(
                opponentsList,
                it,
                newSessionID,
                opponentsIDsString,
                opponentNamesString,
                isVideoCall,
                ""
            )
        }

        CallActivity.start(requireContext(), false, "")
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
}
