package com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.bumptech.glide.Glide
import com.example.sameteam.BuildConfig
import com.example.sameteam.R
import com.example.sameteam.amazonS3.S3Util
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.ActivityCreateTeamBinding
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.AllUsersFragVM
import com.example.sameteam.quickBlox.QbChatDialogMessageListenerImpl
import com.example.sameteam.quickBlox.QbDialogHolder
import com.example.sameteam.quickBlox.VerboseQbChatConnectionListener
import com.example.sameteam.quickBlox.base.BaseActivity2
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.quickBlox.util.shortToast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBIncomingMessagesManager
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBChatDialogMessageListener
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.core.request.GenericQueryRule
import com.quickblox.core.request.QBPagedRequestBuilder
import com.quickblox.messages.QBPushNotifications
import com.quickblox.messages.model.QBEvent
import com.quickblox.messages.model.QBNotificationType
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser
import org.jivesoftware.smack.ConnectionListener
import org.json.JSONObject
import java.io.File

class CreateTeamActivity : BaseActivity2(), DialogsManager.ManagingDialogsCallbacks,
    UserDirectoryAdapter.InvitePeopleListener {
    private val TAG = "CreateTeamActivity"

    lateinit var binding: ActivityCreateTeamBinding
    lateinit var createTeamVM: AllUsersFragVM

    private var dialogsManager: DialogsManager = DialogsManager()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    private lateinit var incomingMessagesManager: QBIncomingMessagesManager
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private var allDialogsMessagesListener: QBChatDialogMessageListener =
        AllDialogsMessageListener()
    private lateinit var chatConnectionListener: ConnectionListener
    private var skipPagination = 0
    private var checkAdapterInit: Boolean = false

    private lateinit var currentUser: QBUser
    private var currentPage: Int = 0

    private var qbUsersList = ArrayList<QBUser>()
    private var groupUsersList = ArrayList<QBUser>()
    private var groupUsersIdList = ArrayList<Int>()
    private var userModelList = ArrayList<UserModel>()

    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var selectedPath: String? = null
    var imageName: String? = null

    var isEdit = false
    lateinit var qbChatDialog: QBChatDialog
    var selectedUsers = ArrayList<UserModel>()
    var previousSelectedUsers = ArrayList<UserModel>()
    var teamImage = ""

    private var teamContactsIdList = mutableListOf<Int>()
    var teamContactUserList = ArrayList<UserModel>()
    var globalUserList = ArrayList<UserModel>()
    private var errorCount: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_team)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_team)
        createTeamVM = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
        ).get(AllUsersFragVM::class.java)

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        if (!ChatHelper.isLogged()) {
            reloginToChat()
        }

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e(TAG, "Finishing $TAG. Current user is null")
            finish()
        }

        try {
            val gson = Gson()
            val gsonString = intent.getStringExtra("editChatDialog")
            if (!gsonString.isNullOrBlank()) {
                isEdit = true
                qbChatDialog = gson.fromJson(gsonString, QBChatDialog::class.java)
                binding.customToolbar.title.text = getString(R.string.edit_team)
                binding.btnSave.text = getString(R.string.submit)

                loadGroupDetails()
            } else {
                binding.customToolbar.title.text = getString(R.string.create_team)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(R.string.something_went_wrong)
            finish()
        }


        createTeamVM.getParticipantIdList()


        /**
         * General Observer
         */
        createTeamVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE

                        createTeamVM.getParticipantIdListResponseModel.data?.userId_list?.let {
                            teamContactsIdList = it
                        }

                        loadUsersFromQB()

                    }

                    Constants.NAVIGATE -> {
                        createTeam()
                    }

                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener {
            Utils.hideKeyboard(this)
            if (binding.teamName.text.toString().trim().isBlank()) {
                longToast("Please enter team name")
            } else {
                if (groupUsersList.isEmpty()) {
                    longToast("Add contacts to proceed.")
                } else {
                    ///TODO add team id
                    Log.d(TAG, "selected: ${Gson().toJson(groupUsersList)}")
                    Log.d(TAG, "selected: ${Gson().toJson(previousSelectedUsers)}")

                    groupUsersList.map { it.externalId.toInt() }.also { ids ->
                        createTeamVM.addUserToContacts(ids)
                    }

                    if (isEdit) {

                        var groupUserIdList: List<Int>;
                        var previousSelectedIdList: List<Int?>;

                        groupUsersList.map { it.externalId.toInt() }.also { ids ->
                            groupUserIdList = ids;
                        }

                        previousSelectedUsers.map { it.id }.also { ids ->
                            previousSelectedIdList = ids;
                        }

                        createTeamVM.editQBTeam(
                            groupUserIdList,
                            previousSelectedIdList,
                            qbChatDialog.dialogId
                        )
                    }
                }
            }
        }

        binding.imgView.setOnClickListener {
            onProfileImageClick()
        }
        ///Commented because to keep search view visible
//        binding.btnSearch.setOnClickListener {
//            if (binding.searchLayout.root.visibility == View.VISIBLE) {
//                binding.searchLayout.root.visibility = View.GONE
//                binding.txtUserNotFound.visibility = View.GONE
//            } else {
//                binding.searchLayout.root.visibility = View.VISIBLE
//                binding.txtUserNotFound.visibility = View.VISIBLE
//            }
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

                        Glide.with(this)
                            .load(selectedPhotoUri)
                            .error(R.drawable.image_placeholder)
                            .placeholder(R.drawable.image_placeholder)
                            .circleCrop()
                            .into(binding.teamPhoto)


                        selectedPath = selectedPhotoUri?.path
                        imageName = selectedPhotoUri?.lastPathSegment

                    } catch (e: Exception) {
                        e.printStackTrace()
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

                    binding.recView.adapter = UserDirectoryAdapter(
                        this@CreateTeamActivity,
                        teamContactUserList,
                        selectedUsers,
                        ArrayList()
                    )

                    (binding.recView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.text = getString(R.string.search_with_email)

                    binding.txtUserNotFound.visibility =
                        if (teamContactUserList.isEmpty()) View.VISIBLE else View.GONE

                    binding.recView.visibility =
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

                    binding.recView.adapter = UserDirectoryAdapter(
                        this@CreateTeamActivity,
                        searchList,
                        selectedUsers,
                        ArrayList()
                    )

                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.visibility =
                        if (searchList.isEmpty()) View.VISIBLE else View.GONE

                    binding.txtUserNotFound.text =
                        if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                            R.string.search_with_email
                        )

                    binding.recView.visibility =
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
                    if (Patterns.EMAIL_ADDRESS.matcher(string).matches()) ArrayList(
                        globalUserList.filter {
                            it.email.toString().contains(string, ignoreCase = true)
                        })
                    else ArrayList(teamContactUserList.filter {
                        "${it.first_name} ${it.last_name}".contains(
                            string, ignoreCase = true
                        )
                    })

                binding.recView.adapter = UserDirectoryAdapter(
                    this@CreateTeamActivity,
                    searchList,
                    selectedUsers,
                    ArrayList()
                )

                binding.progressBar.visibility = View.GONE

                binding.txtUserNotFound.visibility =
                    if (searchList.isEmpty()) View.VISIBLE else View.GONE

                binding.txtUserNotFound.text =
                    if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                        R.string.search_with_email
                    )

                binding.recView.visibility =
                    if (searchList.isEmpty()) View.GONE else View.VISIBLE

                Utils.hideKeyboard(this)

                true
            } else false
        }

        initChatConnectionListener()

    }

    private fun createTeam() {

        if (isEdit) {
            if (!selectedPath.isNullOrBlank() && !imageName.isNullOrBlank()) {
                uploadTaskImage(selectedPath!!, imageName!!)
            } else {
                editDialog(groupUsersList, binding.teamName.text.toString().trim(), teamImage)
            }
        } else {
            if (!selectedPath.isNullOrBlank() && !imageName.isNullOrBlank()) {
                uploadTaskImage(selectedPath!!, imageName!!)
            } else {
                createDialog(groupUsersList, binding.teamName.text.toString().trim(), "")
            }
        }

    }

    private fun onProfileImageClick() {
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
                            packageName,
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
            0
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

    private fun uploadTaskImage(mSelectedMediaPath: String, imageFileName: String) {
        binding.progressBar.visibility = View.VISIBLE

        val key = "sameteam/teamImages/$imageFileName"
        val transferUtility = S3Util.getTransferUtility(this)

        var baseUrl = ""
        var bucketName = ""
        if (BuildConfig.FLAVOR == "client") {
            baseUrl = Keys.liveAWSBaseURL()
            bucketName = Keys.liveAWSBucketName()
        } else {
            baseUrl = Keys.developmentAWSBaseURL()
            bucketName = Keys.developmentAWSBucketName()
        }

        val originalImageTransferUtility = transferUtility.upload(
            bucketName,
            key,
            File(mSelectedMediaPath)
        )

        originalImageTransferUtility.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == originalImageTransferUtility.state) {
                    Log.d(TAG, "onStateChanged: ${baseUrl + key}")

                    if (isEdit) {
                        editDialog(groupUsersList, binding.teamName.text.toString().trim(), baseUrl + key)
                    } else {
                        createDialog(
                            groupUsersList,
                            binding.teamName.text.toString().trim(),
                            baseUrl + key
                        )
                    }

                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onError(id: Int, ex: Exception?) {
                binding.progressBar.visibility = View.GONE
                longToast(R.string.something_went_wrong)
            }

        })
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
                        longToast(getString(R.string.something_went_wrong))
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

    private fun initChatConnectionListener() {
        val rootView: View = findViewById(R.id.teamLayout)
        chatConnectionListener = object : VerboseQbChatConnectionListener(rootView) {
            override fun reconnectionSuccessful() {
                super.reconnectionSuccessful()
                skipPagination = 0
                if (qbChatDialog.type == QBDialogType.GROUP || qbChatDialog.type == QBDialogType.PUBLIC_GROUP) {
                    checkAdapterInit = false
                    // Join active room if we're in Group Chat
                    runOnUiThread {
                        joinGroupChat()
                    }
                }
            }
        }
    }

    private fun notifyUsersAboutCreatingDialog() {
        if (intent.getBooleanExtra(Constants.EXTRA_IS_NEW_DIALOG, false)) {
            dialogsManager.sendMessageCreatedDialog(qbChatDialog)
            intent.removeExtra(Constants.EXTRA_IS_NEW_DIALOG)
        }
    }


    private fun joinGroupChat() {
        binding.progressBar.visibility = View.VISIBLE
        ChatHelper.join(qbChatDialog, object : QBEntityCallback<Void> {
            override fun onSuccess(result: Void?, b: Bundle?) {
                Log.d(TAG, "Joined to Dialog Successful")
                notifyUsersAboutCreatingDialog()
                binding.progressBar.visibility = View.GONE
            }

            override fun onError(e: QBResponseException) {
                Log.d(TAG, "Joining Dialog Error: " + e.message)
                binding.progressBar.visibility = View.GONE
            }
        })
    }

    private fun checkPlayServicesAvailable() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    this, resultCode,
                    Constants.PLAY_SERVICES_REQUEST_CODE
                )?.show()
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
        rules.add(GenericQueryRule(Constants.ORDER_RULE, Constants.ORDER_VALUE_UPDATED_AT))

        val requestBuilder = QBPagedRequestBuilder()
        requestBuilder.rules = rules
        requestBuilder.perPage = Constants.USERS_PAGE_SIZE
        requestBuilder.page = currentPage

        loadTeamContactUsers(requestBuilder)
        loadGlobalUsers(requestBuilder)
    }

    private fun loadUsersWithTag(requestBuilder: QBPagedRequestBuilder) {

        val loggedUser = SharedPrefs.getUser(this)
        val arrayList = listOf(loggedUser?.company?.company_code)

        if (!arrayList.isNullOrEmpty()) {
            QBUsers.getUsers(requestBuilder)
                .performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
                    override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {

                        qbUsersList = ArrayList(usersList)

                        val gson = Gson()
                        for (temp in usersList) {
                            if (temp.id != currentUser.id && !temp.tags.isNullOrEmpty()) {
                                val newUser = gson.fromJson(temp.customData, UserModel::class.java)
                                userModelList.add(newUser)
                            }
                        }

                        binding.recView.layoutManager = LinearLayoutManager(this@CreateTeamActivity)
                        binding.recView.adapter = UserDirectoryAdapter(
                            this@CreateTeamActivity,
                            userModelList,
                            selectedUsers,
                            ArrayList()
                        )

                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onError(e: QBResponseException) {
                        binding.progressBar.visibility = View.GONE
                        currentPage -= 1
                        longToast(R.string.something_went_wrong)
                    }
                })
        } else {
            binding.progressBar.visibility = View.GONE
            longToast(getString(R.string.something_went_wrong))
        }

    }


    private fun loadGlobalUsers(requestBuilder: QBPagedRequestBuilder) {

        QBUsers.getUsers(requestBuilder).performAsync(object : QBEntityCallback<ArrayList<QBUser>> {
            override fun onSuccess(usersList: ArrayList<QBUser>, params: Bundle?) {
                errorCount = 0
                globalUserList = ArrayList()

                qbUsersList = ArrayList(usersList)

                if (usersList.contains(currentUser)) {
                    usersList.remove(currentUser)
                }

                QbUsersDbManager.saveAllUsers(usersList, true)

                val gson = Gson()
                for (item in usersList) {
                    if (item.id != currentUser.id && !item.tags.isNullOrEmpty()) {

                        val newUser = gson.fromJson(
                            item.customData, UserModel::class.java
                        )

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
                        val gson = Gson()
                        for (temp in usersList) {
                            if (temp.id != currentUser.id && !temp.tags.isNullOrEmpty()) {
                                val newUser = gson.fromJson(temp.customData, UserModel::class.java)
                                teamContactUserList.add(newUser)
                            }
                        }

                        binding.recView.layoutManager = LinearLayoutManager(this@CreateTeamActivity)
                        binding.recView.adapter = UserDirectoryAdapter(
                            this@CreateTeamActivity,
                            teamContactUserList,
                            selectedUsers,
                            ArrayList()
                        )

                        binding.progressBar.visibility = View.GONE
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

    private fun createDialog(qbUsers: ArrayList<QBUser>, chatName: String, teamImage: String?) {
        binding.progressBar.visibility = View.VISIBLE

        val groupImage = if (teamImage.isNullOrBlank()) "" else teamImage
        val groupDesc = if (binding.description.text.toString()
                .isBlank()
        ) "" else binding.description.text.toString()

        ChatHelper.createDialogWithSelectedUsers(qbUsers,
            chatName,
            groupImage,
            groupDesc,
            false,
            false,
            object : QBEntityCallback<QBChatDialog> {
                override fun onSuccess(dialog: QBChatDialog, args: Bundle?) {
                    Log.d(TAG, "Creating Dialog Successful")
//                    isProcessingResultInProgress = false
                    registerQbChatListeners()

                    dialogsManager.sendSystemMessageAboutCreatingDialog(
                        systemMessagesManager,
                        dialog
                    )
                    dialogsManager.sendMessageCreatedDialog(dialog)
                    val dialogs = ArrayList<QBChatDialog>()
                    dialogs.add(dialog)
                    QbDialogHolder.addDialogs(dialogs)
//                    DialogJoinerAsyncTask(this@UserDirectoryActivity, dialogs).execute()

                    sendNotification(chatName, qbUsers)

                    val intent = Intent(this@CreateTeamActivity, ChatActivity::class.java)
                    val gson = Gson()
                    intent.putExtra("chatDialog", gson.toJson(dialog))
                    startActivity(intent)
                    finish()
                }

                override fun onError(error: QBResponseException) {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "Creating Dialog Error: " + error.message)
                    longToast(R.string.dialogs_creation_error)
                }
            }
        )
    }

    private fun editDialog(qbUsers: ArrayList<QBUser>, chatName: String, teamImage: String?) {
        binding.progressBar.visibility = View.VISIBLE

        val groupName = if (chatName.isBlank()) "" else chatName
        val groupImage = if (teamImage.isNullOrBlank()) "" else teamImage
        val groupDesc = if (binding.description.text.toString()
                .isBlank()
        ) "" else binding.description.text.toString()

        if (::qbChatDialog.isInitialized && qbChatDialog.dialogId != null) {
            val existingOccupants = qbChatDialog.occupants
            val newUserIds = ArrayList<Int>()
            val oldUserIds = ArrayList<Int>()

            for (user in qbUsers) {
                if (!existingOccupants.contains(user.id)) {
                    newUserIds.add(user.id!!)
                }
            }
            for (id in existingOccupants) {
                if (!groupUsersIdList.contains(id) && id != currentUser.id) {
                    oldUserIds.add(id)
                }
            }

            ChatHelper.getDialogById(
                qbChatDialog.dialogId,
                object : QBEntityCallback<QBChatDialog> {
                    override fun onSuccess(qbChatDialog: QBChatDialog, p1: Bundle?) {
                        dialogsManager.sendMessageRemovedUsers(qbChatDialog, oldUserIds)
                        dialogsManager.sendMessageAddedUsers(qbChatDialog, newUserIds)

                        //   sendNotificationByIds(chatName, newUserIds)

                        dialogsManager.sendSystemMessageRemovedUser(
                            systemMessagesManager,
                            qbChatDialog,
                            oldUserIds
                        )
                        dialogsManager.sendSystemMessageAddedUser(
                            systemMessagesManager,
                            qbChatDialog,
                            newUserIds
                        )

                        qbChatDialog.let {
                            this@CreateTeamActivity.qbChatDialog = it
                        }
                        updateDialog(qbUsers, groupName, groupImage, groupDesc)
                    }

                    override fun onError(e: QBResponseException?) {
                        longToast(R.string.something_went_wrong)
                        binding.progressBar.visibility = View.GONE
                    }
                })

        } else {
            shortToast(R.string.something_went_wrong)
            binding.progressBar.visibility = View.GONE
        }

    }

    private fun updateDialog(
        selectedUsers: ArrayList<QBUser>,
        groupName: String?,
        groupImage: String?,
        groupDesc: String?
    ) {
        Log.d(TAG, "updateDialog: ${selectedUsers.toString()}")
        Log.d(TAG, "updateDialog Image: ${groupImage.toString()}")
        val customData = qbChatDialog.customData
        val desc = customData.getString("description")
        Log.d(TAG, "Dialog Description: $desc")
        Log.d(TAG, "Dialog Edited Description: $groupDesc")
        var changeDescription=false
        changeDescription = desc != groupDesc
        ChatHelper.updateDialog(
            qbChatDialog,
            selectedUsers,
            groupName,
            groupImage,
            groupDesc,
            changeDescription,
            object : QBEntityCallback<QBChatDialog> {
                override fun onSuccess(dialog: QBChatDialog, args: Bundle?) {
                    binding.progressBar.visibility = View.GONE
                    val intent = Intent(this@CreateTeamActivity, ChatActivity::class.java)
                    val gson = Gson()
                    intent.putExtra("chatDialog", gson.toJson(dialog))
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                }

                override fun onError(e: QBResponseException) {
                    binding.progressBar.visibility = View.GONE
                    e.printStackTrace()
                    longToast(R.string.chat_info_add_people_error)
                }
            })
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

        override fun processError(e: QBChatException, qbChatMessage: QBChatMessage) {}
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

    override fun onDialogUpdated(chatDialog: String) {   }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {
    }

    override fun onInvite(participant: UserModel) {
        val gson = Gson()
        for (qbUser in qbUsersList) {
            val tempUser = gson.fromJson(qbUser.customData, UserModel::class.java)
            if (tempUser != null && tempUser.id == participant.id) {
                groupUsersList.add(qbUser)
                groupUsersIdList.add(qbUser.id)
                selectedUsers.add(tempUser)
                return
            }
        }
    }

    override fun onRemove(participant: UserModel) {
        val gson = Gson()
        for (qbUser in qbUsersList) {
            val tempUser = gson.fromJson(qbUser.customData, UserModel::class.java)
            if (tempUser != null && tempUser.id == participant.id) {
                groupUsersList.remove(qbUser)
                groupUsersIdList.remove(qbUser.id)

                for (user in selectedUsers) {
                    if (user.id == tempUser.id) {
                        selectedUsers.remove(user)
                        return
                    }
                }

                return
            }
        }


    }

    private fun loadGroupDetails() {
        if (!qbChatDialog.photo.isNullOrBlank()) {
            Glide.with(this)
                .load(qbChatDialog.photo)
                .error(R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .circleCrop()
                .into(binding.teamPhoto)

            teamImage = qbChatDialog.photo
        }

        binding.teamName.setText(qbChatDialog.name)

        if (qbChatDialog.customData != null) {
            val customData = qbChatDialog.customData
            val desc = customData.getString("description")
            if (!desc.isNullOrBlank()) binding.description.setText(desc)
        }

        selectedUsers.clear()
        previousSelectedUsers.clear()
        groupUsersList.clear()
        groupUsersIdList.clear()
        for (id in qbChatDialog.occupants) {
            val qbUser = QbUsersDbManager.getUserById(id)
            if (qbUser != null && !qbUser.customData.isNullOrBlank() && qbUser.id != currentUser.id) {
                groupUsersList.add(qbUser)
                groupUsersIdList.add(qbUser.id)
                val tempUser = Utils.getUserFromQBUser(qbUser.customData)
                selectedUsers.add(tempUser)
                previousSelectedUsers.add(tempUser)
            }
        }

    }

    /**
     * Send Chat notification
     */
    private fun sendNotification(groupName: String?, selectedUsers: ArrayList<QBUser>) {
        val qbEvent = QBEvent()
        qbEvent.notificationType = QBNotificationType.PUSH
        qbEvent.environment = Constants.QBEnvironment
        val json = JSONObject()
        json.put("notification_type", "GroupChat")


        json.put("title", "Team Notification")
        json.put("subText", groupName)
        json.put("body", "You have been added to new team ${groupName}.")

        qbEvent.message = json.toString()
        val userIds = StringifyArrayList(getIdsSelectedOpponents(selectedUsers))
        qbEvent.userIds = userIds

        QBPushNotifications.createEvents(qbEvent)
            .performAsync(object : QBEntityCallback<List<QBEvent>> {
                override fun onSuccess(p0: List<QBEvent>?, p1: Bundle?) {
                    Log.d("FCM", "onSuccess: ")
                }

                override fun onError(p0: QBResponseException?) {
                    Log.d("FCM", "onError: ${p0?.printStackTrace()}")
//                sendNotification(chatMessage, isGroup , groupName)
                }
            })
    }

    /**
     * Send Chat notification
     */
    private fun sendNotificationByIds(groupName: String?, selectedUsers: ArrayList<Int>) {
        val qbEvent = QBEvent()
        qbEvent.notificationType = QBNotificationType.PUSH
        qbEvent.environment = Constants.QBEnvironment
        val json = JSONObject()
        json.put("notification_type", "GroupChat")


        json.put("title", "Team Notification")
        json.put("subText", groupName)
        json.put("body", "You have been added to new team ${groupName}.")

        qbEvent.message = json.toString()
        val userIds = StringifyArrayList(selectedUsers)
        qbEvent.userIds = userIds

        QBPushNotifications.createEvents(qbEvent)
            .performAsync(object : QBEntityCallback<List<QBEvent>> {
                override fun onSuccess(p0: List<QBEvent>?, p1: Bundle?) {
                    Log.d("FCM", "onSuccess: ")
                }

                override fun onError(p0: QBResponseException?) {
                    Log.d("FCM", "onError: ${p0?.printStackTrace()}")
                }
            })
    }

    private fun getIdsSelectedOpponents(selectedUsers: ArrayList<QBUser>): ArrayList<Int> {
        val opponentsIds = ArrayList<Int>()
        if (selectedUsers.isNotEmpty()) {
            for (qbUser in selectedUsers) {
                opponentsIds.add(qbUser.id)
            }
        }
        return opponentsIds
    }

}