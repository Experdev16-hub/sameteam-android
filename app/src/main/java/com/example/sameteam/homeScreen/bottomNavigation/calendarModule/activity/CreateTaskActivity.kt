package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Strings
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sameteam.BR
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.ActivityCreateTaskBinding
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet.*
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.RemindMeModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel.CreateTaskVM
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.messages.d.i
import com.quickblox.users.model.QBUser
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CreateTaskActivity : BaseActivity<ActivityCreateTaskBinding>(),
    UserDirectoryAdapter.InvitePeopleListener, InvitePeopleBottomSheet.AllPeopleSelectedListener,
    RepeatTimesBottomSheet.TimesDoneListener, SpecificDateBottomSheet.SpecificDateListener,
    SelectTaskTimeBottomSheet.TaskTimeListener, UserDirectoryAdapter.InviteTeamListener,
    RepeatTaskBottomSheet.NeverClickedListener, CustomRepeatTaskBottomSheet.CustomListener,
    RemindMeBottomSheet.RemindMeListener, DialogsManager.ManagingDialogsCallbacks,
    ConfirmDialog.ConfirmClickListener {

    private val TAG = "CreateTaskActivity"

    override fun layoutID() = R.layout.activity_create_task

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(CreateTaskVM::class.java)

    lateinit var binding: ActivityCreateTaskBinding
    lateinit var createTaskVM: CreateTaskVM

    lateinit var fragment: Fragment
    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var dateValue = ""
    var monthValue = ""
    var yearValue = ""
    var weekList = ArrayList<String>()

    private var eventId: Int? = null

    lateinit var adapter: ArrayAdapter<String>
    private var eventMap: HashMap<Int?, String?> = HashMap()
    var eventTitleMap: HashMap<String?, Int?> = HashMap()
    var participantsList = ArrayList<UserModel>()
    var selectedUserList = ArrayList<UserModel>()
    var participantsIds = mutableSetOf<Int>()
    lateinit var taskDetails: TaskDetailsResponseModel.Data
    var isEdit = false
    val unitMap = hashMapOf("m" to "minutes", "h" to "hours", "d" to "day")

    var qbChatDialog: QBChatDialog? = null
    private var currentUser: QBUser? = null
    lateinit var qbDialogUsers: ArrayList<QBUser>
    lateinit var qbDialogUserIds: ArrayList<Int>
    private var dialogsManager: DialogsManager = DialogsManager()
    private lateinit var systemMessagesManager: QBSystemMessagesManager
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private var teamNames = kotlin.collections.ArrayList<String>()
    private var qbTeamIdsList = kotlin.collections.ArrayList<String>()
    private var mInterstitialAd: InterstitialAd? = null

    override fun initActivity(mBinding: ViewDataBinding) {
        binding = mBinding as ActivityCreateTaskBinding
        createTaskVM = getViewModel() as CreateTaskVM

        //Get Events and participants API Call
        createTaskVM.getEventsAndParticipants()
        eventId = intent.getIntExtra("eventId", -1)

        if (SharedPrefsHelper.hasQbUser())
            currentUser = SharedPrefsHelper.getQbUser()
        else {
            showMessage(getString(R.string.something_went_wrong))
            finish()
        }

        if (QBChatService.getInstance() != null && QBChatService.getInstance().systemMessagesManager != null) {
            systemMessagesManager = QBChatService.getInstance().systemMessagesManager
            systemMessagesManager.addSystemMessageListener(systemMessagesListener)
        }

        // binding.taskTitle.filters = arrayOf(Utils.EMOJI_FILTER)
        // binding.description.filters = arrayOf(Utils.EMOJI_FILTER)
        binding.location.filters = arrayOf(Utils.EMOJI_FILTER)


        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.title.text = getString(R.string.activity)
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }

        binding.startTime.isSelected = true
        binding.endTime.isSelected = true
        binding.description.setOnTouchListener { view, event ->
            var lineCount = binding.description.lineCount
            if (lineCount > 3) {
                view.parent.requestDisallowInterceptTouchEvent(true)
                if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
                return@setOnTouchListener false
            }
            return@setOnTouchListener false
        }

        /**
         * Check if any task comes for edit task and load task details
         */
        val gson = Gson()
        val gsonString = intent.getStringExtra("taskDetails")
        if (!gsonString.isNullOrBlank()) {
            isEdit = true
            binding.btnCreateTask.text = getString(R.string.submit)
            val type: Type = object : TypeToken<TaskDetailsResponseModel.Data>() {}.type
            taskDetails = gson.fromJson(gsonString, type)
            binding.startTeamLayout.visibility = View.GONE
            loadTaskDetails()
        }



        if (createTaskVM.createTaskRequestModel.remind_me.isEmpty())
            createTaskVM.createTaskRequestModel.remind_me = arrayListOf(RemindMeModel("1", "d"))

        if (SharedPrefs.getUser(this)?.plan_upgrade == false)
            loadInterstitialAd()

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)
        layoutManager.stackFromEnd = true
        binding.recView.layoutManager = layoutManager
        binding.recView.adapter = BindingAdapter(
            layoutId = R.layout.participants_user_card,
            br = BR.model,
            list = selectedUserList,
            clickListener = { view, position ->
                when (view.id) {
                    R.id.btnRemove -> {

                        val user = selectedUserList[position]

                        if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
                            val tempUser = QbUsersDbManager.getQBUserByExternalId(user.id)

                            if (tempUser != null && qbDialogUserIds.contains(tempUser.id)) {
                                qbDialogUserIds.remove(tempUser.id)
                            }
                            if (tempUser != null && qbDialogUsers.contains(tempUser)) {
                                qbDialogUsers.remove(tempUser)
                            }
                        }

                        selectedUserList.removeAt(position)
                        participantsIds.remove(user.id)
                        binding.recView.adapter?.notifyDataSetChanged()

                        if (selectedUserList.isEmpty()) {
                            binding.userList.visibility = View.GONE

                        } else {
                            binding.userList.visibility = View.VISIBLE
                        }
                    }
                }
            }
        )

        binding.taskImage.setOnClickListener {
            createTaskVM.onTaskImageClicked(it)
        }

        binding.locationInput.setEndIconOnClickListener {
            val confirmDialog = ConfirmDialog(
                this,
                "Be specific so team members signed up for activities know where to be. Examples: Address, Facility, Room, Booth, Sports Field, etc.",
                "AlreadyCompleted"
            )
            confirmDialog.show(supportFragmentManager, "Confirm")
        }

        binding.btnInvite.setOnClickListener {
            fragment = InvitePeopleBottomSheet(this, participantsList, selectedUserList)
            (fragment as InvitePeopleBottomSheet).show(
                supportFragmentManager,
                InvitePeopleBottomSheet::class.java.name
            )
        }

        binding.setReminders.setOnClickListener {
            fragment = RemindMeBottomSheet(
                createTaskVM.createTaskRequestModel.remind_me
            )
            (fragment as RemindMeBottomSheet).show(
                supportFragmentManager,
                RemindMeBottomSheet::class.java.name
            )
        }

//        binding.txtRepeat.setOnClickListener {
//            /**
//             * Start date of task should be selected before custom repeat
//             */
//            Utils.hideKeyboard(this)
//            if (binding.startDate.text.isNullOrBlank()) {
//                showMessage("Please select start date")
//            } else {
//                val localDate = getDateFromString(binding.startDate.text.toString())
//                fragment = RepeatTaskBottomSheet(localDate)
//                (fragment as RepeatTaskBottomSheet).show(
//                    supportFragmentManager,
//                    RepeatTaskBottomSheet::class.java.name
//                )
//            }
//
//        }

//        binding.btnInfo.setOnClickListener {
//            val confirmDialog = ConfirmDialog(this,
//                "Create a team to share tasks, chat, and/or make audio and video calls.", "AlreadyCompleted")
//            confirmDialog.show(supportFragmentManager, "Confirm")
//        }

        binding.startTime.setOnClickListener {
            Utils.hideKeyboard(this)
            fragment = SelectTaskTimeBottomSheet(Constants.START_DATE)
            (fragment as SelectTaskTimeBottomSheet).show(
                supportFragmentManager,
                SelectTaskTimeBottomSheet::class.java.name
            )
        }

        binding.endTime.setOnClickListener {
            Utils.hideKeyboard(this)
            fragment = SelectTaskTimeBottomSheet(Constants.END_DATE)
            (fragment as SelectTaskTimeBottomSheet).show(
                supportFragmentManager,
                SelectTaskTimeBottomSheet::class.java.name
            )
        }

        binding.allDayCheckBox.setOnCheckedChangeListener { _, isChecked ->

            //No use of start & end date if "ALL Day" option is selected
            if (isChecked) {
                binding.timeLayout.visibility = View.GONE
                createTaskVM.createTaskRequestModel.all_day = true
            } else {
                binding.timeLayout.visibility = View.VISIBLE
                createTaskVM.createTaskRequestModel.all_day = false
            }
        }

//        binding.startTeamCheckBox.setOnCheckedChangeListener { _, isChecked ->
//            createTaskVM.createTaskRequestModel.is_start_team = isChecked
//        }

        binding.btnToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            createTaskVM.createTaskRequestModel.is_private = isChecked
        }

        binding.startDate.setOnClickListener {

            Utils.hideKeyboard(this)
            val fragment = SpecificDateBottomSheet(Constants.START_DATE)
            fragment.show(
                supportFragmentManager,
                SpecificDateBottomSheet::class.java.name
            )
        }

        /**
         * Event spinner listener
         */
        binding.event.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                Log.d(TAG, "onItemClick: $position ${binding.event.adapter.getItem(position)}")
                createTaskVM.createTaskRequestModel.event_id =
                    eventTitleMap.getValue(binding.event.adapter.getItem(position).toString())
                        .toString()
            }

        binding.event.setOnClickListener {
            if (eventMap.isEmpty()) {
                showMessage("No event found")
            }
        }

        /**
         * Participants and teams info button clicked
         */
        binding.btnInfoParticipants.setOnClickListener {
            val confirmDialog = ConfirmDialog(
                this,
                "Add yourself to view the activity on My Calendar then you must tap accept to take up a slot. Otherwise, the activity will only be viewable in Team Calendar.",
                "AlreadyCompleted"
            )
            confirmDialog.show(supportFragmentManager, "Confirm")
        }

        /**
         * Create Task button clicked
         */
        binding.btnCreateTask.setOnClickListener {
            Utils.hideKeyboard(this)
            if (!createTaskVM.createTaskRequestModel.repeat_type.isNullOrBlank()) {
                when (createTaskVM.createTaskRequestModel.repeat_type) {
                    "day" -> {
                        createTaskVM.createTaskRequestModel.repeat_value = "everyday"
                    }

                    "week" -> {
                        createTaskVM.createTaskRequestModel.repeat_value =
                            TextUtils.join(",", weekList)
                    }

                    "month" -> {
                        createTaskVM.createTaskRequestModel.repeat_value = dateValue
                    }

                    "year" -> {
                        createTaskVM.createTaskRequestModel.repeat_value =
                            "$dateValue-$monthValue"
                    }
                }
            }
            if (createTaskVM.createTaskRequestModel.all_day == true) {
                createTaskVM.createTaskRequestModel.start_time = ""
                createTaskVM.createTaskRequestModel.end_time = ""
            }

            val tempArrayList = ArrayList<Int>()
            tempArrayList.addAll(participantsIds)
            createTaskVM.createTaskRequestModel.participant_ids = tempArrayList

            if (binding.edtSlots.text.toString().isNotBlank())
                createTaskVM.createTaskRequestModel.total_slots =
                    binding.edtSlots.text.toString().toInt()

            if (teamNames.isNotEmpty())
                createTaskVM.createTaskRequestModel.team_name = teamNames.joinToString(", ")

            if (qbTeamIdsList.isNotEmpty())
                createTaskVM.createTaskRequestModel.qb_team_id = qbTeamIdsList.joinToString(", ")

            createTaskVM.onCreateClicked()

        }


        /**
         * General observer
         */
        createTaskVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.CAMERA_INTENT -> {
                        launchCameraIntent()
                    }

                    Constants.GALLERY_INTENT -> {
                        launchGalleryIntent()
                    }

                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.SHOW_PROGRESS -> {
                        showSimpleProgress(binding.btnCreateTask)
                    }

                    Constants.HIDE_PROGRESS -> {
                        if (isEdit)
                            hideProgressDialog(
                                binding.btnCreateTask,
                                getString(R.string.submit)
                            )
                        else
                            hideProgressDialog(
                                binding.btnCreateTask,
                                getString(R.string.create_activity)
                            )
                    }

                    Constants.NAVIGATE -> {
                        Log.d(TAG, "initActivity: Task Id ${createTaskVM.taskId}")
                        if (mInterstitialAd != null) {
                            mInterstitialAd?.show(this)
                        } else {
                            Log.d("TAG", "The interstitial ad wasn't ready yet.")
                            onBackPressed()
                        }

                    }

                    "CheckTime" -> {
                        checkValidTime()
                    }

                    "ImageUploaded" -> {
                        //Create Quickblox Team
                        if (isEdit) {
                            createTaskVM.callEditTaskAPI()
                        } else {
//                            if (participantsIds.isNullOrEmpty())
//                                createTaskVM.callCreateTaskAPI()
//                            else
//                                createTeam()

                            createTaskVM.callCreateTaskAPI()
                        }

                    }

                    else -> {
                        binding.progressBar.visibility = View.GONE
                        if (isEdit)
                            hideProgressDialog(
                                binding.btnCreateTask,
                                getString(R.string.submit)
                            )
                        else
                            hideProgressDialog(
                                binding.btnCreateTask,
                                getString(R.string.create_activity)
                            )
                        showMessage(it)
                    }
                }
            }
        }

        /**
         * Observe changes and set events list from API call
         */
        createTaskVM.observeEventList().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (!it.isNullOrEmpty()) {
                    eventMap = HashMap()
                    eventTitleMap = HashMap()
                    for (item in it) {
                        eventMap[item.id] = item.title
                        eventTitleMap[item.title] = item.id
                    }
                    adapter =
                        ArrayAdapter(this, R.layout.list_item, R.id.txt, eventMap.values.toList())
                    binding.event.setAdapter(adapter)

                    if (eventMap.contains(eventId)) {
                        createTaskVM.createTaskRequestModel.event_id = eventId.toString()
                        binding.event.setText(eventMap[eventId])
                    }
                }
            }
        }

        /**
         * Observe changes and set participants list from API call
         */
        createTaskVM.observeParticipants().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (!it.isNullOrEmpty()) {
                    participantsList = it
                }
            }
        }

        /**
         * Updated version of onActivityResult
         */
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
                            .into(binding.taskImage)


                        createTaskVM.selectedPath = selectedPhotoUri?.path
                        createTaskVM.imageName = selectedPhotoUri?.lastPathSegment

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            getString(R.string.interstitial_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    loadInterstitialCallbacks()
                }
            })

    }

    private fun loadInterstitialCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d(TAG, "Ad dismissed fullscreen content.")
                mInterstitialAd = null
                onBackPressed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                Log.e(TAG, "Ad failed to show fullscreen content.")
                mInterstitialAd = null
                onBackPressed()
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterQbChatListeners()
    }

    /**
     * Check if start time is not more than end time
     */
    private fun checkValidTime() {
        if (createTaskVM.createTaskRequestModel.all_day == false && (timeStringToDate(binding.startTime.text.toString()) >= timeStringToDate(
                binding.endTime.text.toString()
            ))
        ) {
            showMessage(getString(R.string.enter_valid_time))
            if (isEdit)
                hideProgressDialog(binding.btnCreateTask, getString(R.string.submit))
            else
                hideProgressDialog(binding.btnCreateTask, getString(R.string.create_activity))
        } else {
            val startDate = createTaskVM.createTaskRequestModel.start_date
            val mTimeZone =
                SimpleDateFormat("Z", Locale.getDefault()).format(System.currentTimeMillis())

            if (createTaskVM.createTaskRequestModel.all_day == true) {
                Log.d(TAG, "checkValidTime: ${createTaskVM.createTaskRequestModel}")

                val startTime = "12:00 am"
                val endTime = "11:59 pm"

                val startDateTimeString = startDate + "T" + startTime + ".000" + mTimeZone
                val endDateTimeString = startDate + "T" + endTime + ".000" + mTimeZone

                createTaskVM.createTaskRequestModel.start_time_stamp =
                    localToUTCTimestamp(startDateTimeString)
                createTaskVM.createTaskRequestModel.end_time_stamp =
                    localToUTCTimestamp(endDateTimeString)

//                Log.d(TAG, "checkValidTime Start: ${localToUTCTimestamp(startDateTimeString)}")
//                Log.d(TAG, "checkValidTime End :  ${localToUTCTimestamp(endDateTimeString)}")
            } else {
                val startTime = binding.startTime.text
                val endTime = binding.endTime.text

                val startDateTimeString = startDate + "T" + startTime + ".000" + mTimeZone
                val endDateTimeString = startDate + "T" + endTime + ".000" + mTimeZone

                createTaskVM.createTaskRequestModel.start_time_stamp =
                    localToUTCTimestamp(startDateTimeString)
                createTaskVM.createTaskRequestModel.end_time_stamp =
                    localToUTCTimestamp(endDateTimeString)

//                Log.d(TAG, "checkValidTime Start: ${localToUTCTimestamp(startDateTimeString)}")
//                Log.d(TAG, "checkValidTime End :  ${localToUTCTimestamp(endDateTimeString)}")
            }

            val utcDate =
                createTaskVM.createTaskRequestModel.start_time_stamp?.let { Date(it * 1000) }
            val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val utcFormattedDate = sdf.format(utcDate)

            Log.d(TAG, "checkValidTime local :  ${startDate}")
            Log.d(TAG, "checkValidTime UTC  :  ${utcFormattedDate}")
            if (utcFormattedDate != startDate) {
                if (createTaskVM.createTaskRequestModel.repeat_type == "week") {
                    Log.d(
                        TAG,
                        "checkValidTime Old :  ${Gson().toJson(TextUtils.join(",", weekList))}"
                    )
                    val newWeekList = ArrayList<String>()
                    val local = SimpleDateFormat(
                        "Z",
                        Locale.getDefault()
                    ).format(System.currentTimeMillis())
                    Log.d(TAG, "initActivity Timezone: $local")
                    if (local[0].toString() == "+") {
                        for (i in weekList) {
                            if (i == "1") {
                                newWeekList.add("7")
                            } else {
                                newWeekList.add((i.toInt() - 1).toString())
                            }
                        }
                    } else {
                        for (i in weekList) {
                            if (i == "7") {
                                newWeekList.add("1")
                            } else {
                                newWeekList.add((i.toInt() + 1).toString())
                            }
                        }
                    }

                    Log.d(
                        TAG,
                        "checkValidTime New :  ${Gson().toJson(TextUtils.join(",", newWeekList))}"
                    )
                    createTaskVM.createTaskRequestModel.repeat_value =
                        TextUtils.join(",", newWeekList)
                    createTaskVM.createTaskRequestModel.repeat_value_local =
                        TextUtils.join(",", weekList)
                }
            }

            /**
             * Upload Image if all validations are correct
             */
            if (!createTaskVM.selectedPath.isNullOrBlank() && !createTaskVM.imageName.isNullOrBlank()) {
                createTaskVM.uploadTaskImage(
                    createTaskVM.selectedPath!!,
                    createTaskVM.imageName!!,
                    this
                )
            } else {
                // if there is no participant then don't create Quickblox Team, call API

                if (isEdit) {
                    createTaskVM.callEditTaskAPI()
                } else {
//                    if(participantsIds.isNullOrEmpty() || createTaskVM.createTaskRequestModel.is_start_team == false)
//                        createTaskVM.callCreateTaskAPI()
//                    else
//                        createTeam()
                    createTaskVM.callCreateTaskAPI()
                }
            }

        }
    }

    private fun timeStringToDate(string: String): LocalTime {
        val dtf = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        return try {
            LocalTime.parse(string, dtf)
        } catch (e: java.lang.Exception) {
            val splitArray = string.split(" ").toTypedArray()
            val ampm = splitArray[1].uppercase(Locale.getDefault())
            val newString = "${splitArray[0]} $ampm"
            LocalTime.parse(newString, dtf)
        }
    }

    /**
     * Override method of UserDirectoryAdapter when user is invited
     */
    override fun onInvite(participant: UserModel) {
        if (!participantsIds.contains(participant.id)) {
            selectedUserList.add(participant)
            participantsIds.add(participant.id!!)
        }
        binding.recView.scrollToPosition(selectedUserList.size - 1)
        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()

        if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
            val tempUser = QbUsersDbManager.getQBUserByExternalId(participant.id)
            if (tempUser != null && !qbDialogUserIds.contains(tempUser.id)) {
                qbDialogUserIds.add(tempUser.id)
                qbDialogUsers.add(tempUser)
            }
        }
        if (selectedUserList.isEmpty()) {
            binding.userList.visibility = View.GONE

        } else {
            binding.userList.visibility = View.VISIBLE
        }
    }

    /**
     * Override method of UserDirectoryAdapter when user is removed
     */
    override fun onRemove(participant: UserModel) {

        for (user in selectedUserList) {
            if (user.id == participant.id) {
                selectedUserList.remove(user)
                participantsIds.remove(participant.id)
                break
            }
        }
        binding.recView.scrollToPosition(selectedUserList.size - 1)
        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()

        if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
            val tempUser = QbUsersDbManager.getQBUserByExternalId(participant.id)
            if (tempUser != null && qbDialogUserIds.contains(tempUser.id)) {
                qbDialogUserIds.remove(tempUser.id)
            }
            if (tempUser != null && qbDialogUsers.contains(tempUser)) {
                qbDialogUsers.remove(tempUser)
            }
        }
        if (selectedUserList.isEmpty()) {
            binding.userList.visibility = View.GONE

        } else {
            binding.userList.visibility = View.VISIBLE
        }
    }

    /**
     * Override method of UserDirectoryAdapter when team is added
     */
    override fun onInvite(chatDialog: QBChatDialog) {

        Log.d(TAG, "EditActivityRequestModel: ${Gson().toJson(chatDialog)}")

        teamNames.add(chatDialog.name)
        qbTeamIdsList.add(chatDialog.dialogId.toString())


        if (!chatDialog.occupants.isNullOrEmpty()) {
            for (id in chatDialog.occupants) {
                //  if (id != currentUser?.id) { // removed this to add logged in user for invitation
                val qbUser = QbUsersDbManager.getUserById(id)
                if (qbUser != null && !qbUser.customData.isNullOrBlank()) {
                    val user = Utils.getUserFromQBUser(qbUser.customData)
                    if (!participantsIds.contains(user.id)) {
                        selectedUserList.add(user)
                        participantsIds.add(user.id!!)
                    }

                    if (::qbDialogUserIds.isInitialized && qbChatDialog != null && !qbDialogUserIds.contains(
                            qbUser.id
                        )
                    ) {
                        qbDialogUserIds.add(qbUser.id)
                        qbDialogUsers.add(qbUser)
                    }
                    //}
                }
            }
        }
        //

        binding.recView.scrollToPosition(selectedUserList.size - 1)
//        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()

        if (selectedUserList.isEmpty()) {
            binding.userList.visibility = View.GONE

        } else {
            binding.userList.visibility = View.VISIBLE
        }
    }

    /**
     * Override method of UserDirectoryAdapter when team is removed
     */
    override fun onRemove(chatDialog: QBChatDialog) {

        teamNames.remove(chatDialog.name)
        qbTeamIdsList.remove(chatDialog.dialogId.toString())

        if (!chatDialog.occupants.isNullOrEmpty()) {
            for (id in chatDialog.occupants) {
                if (id != currentUser?.id) {
                    val qbUser = QbUsersDbManager.getUserById(id)
                    if (qbUser != null && !qbUser.customData.isNullOrBlank()) {
                        val tempUser = Utils.getUserFromQBUser(qbUser.customData)

                        for (user in selectedUserList) {
                            if (user.id == tempUser.id) {
                                selectedUserList.remove(user)
                                participantsIds.remove(user.id)
                                break
                            }
                        }

                        if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
                            if (qbDialogUserIds.contains(qbUser.id)) {
                                qbDialogUserIds.remove(tempUser.id)
                            }
                            if (qbDialogUsers.contains(qbUser)) {
                                qbDialogUsers.remove(qbUser)
                            }
                        }
                    }
                }
            }
        }

        binding.recView.scrollToPosition(selectedUserList.size - 1)
//        createTaskVM.onInviteUserClicked(selectedUserList)
        binding.recView.adapter?.notifyDataSetChanged()
        if (selectedUserList.isEmpty()) {
            binding.userList.visibility = View.GONE

        } else {
            binding.userList.visibility = View.VISIBLE
        }

    }

    /**
     * Override method of InvitePeopleBottomSheet when all users are added/removed
     */
    override fun onAllSelected(tag: Boolean) {
        if (tag) {
            // Add all users
            val newList = ArrayList(participantsList)
            selectedUserList.clear()
            participantsIds.clear()
            selectedUserList.addAll(newList)
            selectedUserList.forEach { participantsIds.add(it.id!!) }
            binding.recView.adapter?.notifyDataSetChanged()
            binding.recView.scrollToPosition(selectedUserList.size - 1)
            createTaskVM.onInviteUserClicked(selectedUserList)

            if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
                qbDialogUserIds.clear()
                qbDialogUsers.clear()

                for (user in participantsList) {
                    val tempUser = QbUsersDbManager.getQBUserByExternalId(user.id)
                    if (tempUser != null && !qbDialogUserIds.contains(tempUser.id)) {
                        qbDialogUserIds.add(tempUser.id)
                        qbDialogUsers.add(tempUser)
                    }
                }
            }

        } else {
            // Remove all users
            selectedUserList.clear()
            participantsIds.clear()
            binding.recView.adapter?.notifyDataSetChanged()
            createTaskVM.onInviteUserClicked(selectedUserList)
            if (::qbDialogUserIds.isInitialized && qbChatDialog != null) {
                qbDialogUserIds.clear()
                qbDialogUsers.clear()
            }
        }

    }


    /**
     * Override method of RemindMeBottomSheet
     */
    override fun onRemind(selectedItems: ArrayList<String>) {
        val arrayList = arrayListOf("5 m", "10 m", "15 m", "30 m", "45 m", "1 h", "2 h", "1 d")

        if (selectedItems.size == 1 && selectedItems[0] == getString(R.string.dont_remind)) {
            binding.txtRemindMe.text = getString(R.string.dont_remind)
        } else {
            var temp = ""
            val remindMeList = ArrayList<RemindMeModel>()
            for (i in arrayList) {
                if (selectedItems.contains(i)) {
                    temp += if (i == "1 h") {
                        remindMeList.add(RemindMeModel("1", "h"))
                        "1 hour, "
                    } else {
                        val splitString = i.split(" ").toTypedArray()
                        remindMeList.add(RemindMeModel(splitString[0], splitString[1]))
                        "${splitString[0]} ${unitMap.getValue(splitString[1])}, "
                    }
                }
            }
            if (temp.isNotBlank()) {
                binding.txtRemindMe.text = "${temp.subSequence(0, temp.length - 2)} before"
                createTaskVM.createTaskRequestModel.remind_me = remindMeList
            }
        }
    }

//    override fun onLocation(value: String) {
//        binding.txtLocation.setText(value)
////        (fragment as LocationBottomSheet).dismiss()
//    }

    /**
     * Override method of RepeatTimesBottomSheet
     */
    override fun onRepeatTimesDone(value: String) {
        createTaskVM.onRepeatTimesClicked(value)
    }

    /**
     * Override method of SpecificDateBottomSheet
     */
    override fun onSpecificDateDone(value: String, location: String) {
        if (location == Constants.START_DATE) {            // Task Start Date
//            binding.startDate.text = value
            binding.startDate.setText(value)

            val array = value.split("-").toTypedArray()
            dateValue = array[0]
            monthValue = array[1]
            yearValue = array[2]

            createTaskVM.createTaskRequestModel.start_date = value

            Log.d(TAG, "onSpecificDateDone: ${createTaskVM.createTaskRequestModel.start_date}")
        } else {
            // Repeat End Date for Task
            createTaskVM.onSpecificDateClicked(value, location)
        }
    }

    /**
     * Override method of SelectTaskTimeBottomSheet
     */
    override fun onTaskTimeDone(value: String, location: String) {
        if (location == Constants.START_DATE) {           // Task Start Time
//            binding.startTime.text = value
            binding.startTime.setText(value)
            Log.d(TAG, "onTaskTimeDoneLocal: $value")

            val serverTime = localToUTC(value)
            Log.d(TAG, "onTaskTimeDone: $serverTime")
            createTaskVM.createTaskRequestModel.start_time = serverTime
            createTaskVM.createTaskRequestModel.startTimeNew=value

        } else {                                           // Task End Time
//            binding.endTime.text = value
            binding.endTime.setText(value)
            Log.d(TAG, "onTaskTimeDoneLocal: $value")

            val serverTime = localToUTC(value)
            Log.d(TAG, "onTaskTimeDone: $serverTime")
            createTaskVM.createTaskRequestModel.end_time = serverTime
            createTaskVM.createTaskRequestModel.endTimeNew=value
        }
    }



    /**
     * Override method of RepeatTaskBottomSheet
     */
    override fun onNeverClicked() {
        //binding.txtRepeat.text = getString(R.string.does_not_repeat)
        createTaskVM.createTaskRequestModel.repeat_task = false
        createTaskVM.createTaskRequestModel.repeat_type = ""
        createTaskVM.createTaskRequestModel.repeat_value = ""
    }


    /**
     * Override method of CustomRepeatTaskBottomSheet
     */
    override fun onCustomSelected(list: ArrayList<String?>, dateString: String?, times: String?) {
        (fragment as RepeatTaskBottomSheet).dismiss()
        Log.d(TAG, "onCustomSelected: $list")

        var repeatString = ""
        weekList.clear()
        createTaskVM.createTaskRequestModel.repeat_task = true

        if (!list.isNullOrEmpty()) {

            when (list[0]) {
                getString(R.string.daily) -> {
                    repeatString = "Repeats every day"
                    createTaskVM.createTaskRequestModel.repeat_type = "day"
                }

                getString(R.string.monthly) -> {
                    repeatString = "Repeats monthly"
                    createTaskVM.createTaskRequestModel.repeat_type = "month"
                }

                getString(R.string.yearly) -> {
                    repeatString = "Repeats yearly"
                    createTaskVM.createTaskRequestModel.repeat_type = "year"
                }

                else -> {
                    repeatString = "Repeats weekly on " + TextUtils.join(", ", list)
                    createTaskVM.createTaskRequestModel.repeat_type = "week"

                    if ("Mon" in list) weekList.add("1")
                    if ("Tue" in list) weekList.add("2")
                    if ("Wed" in list) weekList.add("3")
                    if ("Thu" in list) weekList.add("4")
                    if ("Fri" in list) weekList.add("5")
                    if ("Sat" in list) weekList.add("6")
                    if ("Sun" in list) weekList.add("7")

                }
            }
        }


        if (!dateString.isNullOrBlank()) {
            Log.d(TAG, "onCustomSelected: $dateString")
            repeatString += "; until $dateString"

            createTaskVM.createTaskRequestModel.repeat_end_type = "date"
            createTaskVM.createTaskRequestModel.repeat_end_value = dateString
        } else if (!times.isNullOrBlank()) {
            Log.d(TAG, "onCustomSelected: $times")
            repeatString += if (times == "1") "; for 1 time" else "; for $times times"
            createTaskVM.createTaskRequestModel.repeat_end_type = "times"
            createTaskVM.createTaskRequestModel.repeat_end_value = times
        } else {
            createTaskVM.createTaskRequestModel.repeat_end_type = "never"
            createTaskVM.createTaskRequestModel.repeat_end_value = ""
        }

        Log.d(TAG, "onCustomSelected: $repeatString")
      //  binding.txtRepeat.text = repeatString

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

    /**
     * Load Task details
     */
    private fun loadTaskDetails() {
        binding.progressBar.visibility = View.VISIBLE

        createTaskVM.createTaskRequestModel.id = taskDetails.id
        createTaskVM.createTaskRequestModel.name = taskDetails.name
        createTaskVM.createTaskRequestModel.is_private = taskDetails.is_private
        createTaskVM.createTaskRequestModel.all_day = taskDetails.all_day
        createTaskVM.createTaskRequestModel.remind_me = taskDetails.remind_me
        createTaskVM.createTaskRequestModel.repeat_task = taskDetails.repeat_task
        createTaskVM.createTaskRequestModel.repeat_value = taskDetails.repeat_value
        createTaskVM.createTaskRequestModel.repeat_value_local = taskDetails.repeat_value_local
        createTaskVM.createTaskRequestModel.repeat_type = taskDetails.repeat_type
        createTaskVM.createTaskRequestModel.repeat_end_type = taskDetails.repeat_end_type
        createTaskVM.createTaskRequestModel.repeat_end_value = taskDetails.repeat_end_value
        createTaskVM.createTaskRequestModel.image_url = taskDetails.image_url
//        createTaskVM.createTaskRequestModel.is_start_team = taskDetails.is_start_team
        createTaskVM.createTaskRequestModel.is_start_team = false
        createTaskVM.createTaskRequestModel.qb_team_id = taskDetails.qb_team_id


        if (!taskDetails.location.isNullOrBlank())
            createTaskVM.createTaskRequestModel.location = taskDetails.location


        if (!taskDetails.description.isNullOrBlank())
            createTaskVM.createTaskRequestModel.description = taskDetails.description


        if (taskDetails.event != null) {
            createTaskVM.createTaskRequestModel.event_id = taskDetails.event?.id.toString()
            binding.event.setText(taskDetails.event!!.title)
        }

        binding.edtSlots.isEnabled = false
        binding.edtSlots.setText(taskDetails.total_slots?.toString())

        if (taskDetails.image_url.isNullOrBlank()) {
            Glide.with(this)
                .load(R.drawable.image_placeholder)
                .circleCrop()
                .into(binding.taskImage)
        } else {
            Glide.with(this)
                .load(taskDetails.image_url)
                .error(R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .circleCrop()
                .into(binding.taskImage)
        }



//        if (taskDetails.repeat_task == false) binding.txtRepeat.text =
//            getString(R.string.does_not_repeat)
//        else setRepeatText(
//            taskDetails.repeat_type,
//            taskDetails.repeat_value,
//            taskDetails.repeat_end_type,
//            taskDetails.repeat_end_value
//        )

        binding.allDayCheckBox.isChecked = taskDetails.all_day == true
//        binding.startTeamCheckBox.isChecked = taskDetails.is_start_team == true
//        binding.startTeamCheckBox.isEnabled = taskDetails.is_start_team == false


        if (taskDetails.all_day == true) {
            binding.timeLayout.visibility = View.GONE
            createTaskVM.createTaskRequestModel.all_day = true
            createTaskVM.createTaskRequestModel.start_time = ""
            createTaskVM.createTaskRequestModel.end_time = ""

            val startDate = utcTimestampToLocalDateTime(taskDetails.start_time_stamp.toString())
            if (startDate != null) {
                val localFormattedDate = getFormattedDate(startDate.toLocalDate())
                binding.startDate.setText(localFormattedDate)
                createTaskVM.createTaskRequestModel.start_date = localFormattedDate
            }
        } else {
            binding.timeLayout.visibility = View.VISIBLE
            createTaskVM.createTaskRequestModel.all_day = false
            createTaskVM.createTaskRequestModel.start_time = taskDetails.start_time
            createTaskVM.createTaskRequestModel.end_time = taskDetails.end_time

            createTaskVM.createTaskRequestModel.start_time_stamp = taskDetails.start_time_stamp
            createTaskVM.createTaskRequestModel.end_time_stamp = taskDetails.end_time_stamp

            val startDate = utcTimestampToLocalDateTime(taskDetails.start_time_stamp.toString())
            val endDate = utcTimestampToLocalDateTime(taskDetails.end_time_stamp.toString())

            if (startDate != null) {
                createTaskVM.createTaskRequestModel.startTimeNew = getFormattedTime(startDate.toLocalTime())
            }
            if (endDate != null) {
                createTaskVM.createTaskRequestModel.endTimeNew = getFormattedTime(endDate.toLocalTime())
            }

            Log.d(TAG, "loadTaskDetails: ${taskDetails.start_time_stamp} ${
                startDate?.toLocalDate()
                    ?.let { getFormattedDate(it) }
            }"
            )
            if (startDate != null) {
                binding.startTime.setText(getFormattedTime(startDate.toLocalTime()))
                val localFormattedDate = getFormattedDate(startDate.toLocalDate())
                binding.startDate.setText(localFormattedDate)
                //createTaskVM.createTaskRequestModel.start_date = localFormattedDate
                createTaskVM.createTaskRequestModel.start_date = taskDetails.start_date.toString()

            }
            if (endDate != null) {
                binding.endTime.setText(getFormattedTime(endDate.toLocalTime()))
            }
        }

        binding.btnToggle.isChecked = taskDetails.is_private == true

        val newUsersList = ArrayList<UserModel>()
        participantsIds.clear()
        for (item in taskDetails.task_participants) {
            if (item.user != null) {//&& (taskDetails.user.id != item.user!!.id!!) removed  this from the condition
                newUsersList.add(item.user!!)
                participantsIds.add(item.user!!.id!!)
            }

        }

        selectedUserList.clear()
        participantsIds.clear()
        selectedUserList.addAll(newUsersList)
        selectedUserList.forEach { participantsIds.add(it.id!!) }
        binding.recView.adapter?.notifyDataSetChanged()
        binding.recView.scrollToPosition(selectedUserList.size - 1)

        val tempArrayList = ArrayList<Int>()
        tempArrayList.addAll(participantsIds)
        createTaskVM.createTaskRequestModel.participant_ids = tempArrayList

        taskDetails.team_name?.let { teams ->

            teamNames.clear()
            teamNames = teams.split(", ").map { teams.trim() } as ArrayList<String>

        }
        taskDetails.qb_team_id?.let { id ->

            qbTeamIdsList.clear()
            qbTeamIdsList = id.split(", ").map { id.trim() } as ArrayList<String>

        }

        var txtTime = ""

        for (item in taskDetails.remind_me) {
            if (item.value == "1" && item.unit == "h") txtTime += "1 hour, "
            else {
                txtTime += "${item.value} ${unitMap.getValue(item.unit.toString())}, "
            }
        }

        if (txtTime.isNotBlank())
            binding.txtRemindMe.text = "${txtTime.subSequence(0, txtTime.length - 2)} before"

        /**
         * Load Group details associated from that task
         */
        if (!taskDetails.qb_team_id.isNullOrBlank()) {
            ChatHelper.getDialogById(
                taskDetails.qb_team_id!!,
                object : QBEntityCallback<QBChatDialog> {
                    override fun onSuccess(p0: QBChatDialog?, p1: Bundle?) {
                        if (p0 != null) {
                            qbChatDialog = p0
                            ChatHelper.getUsersFromDialog(
                                p0,
                                object : QBEntityCallback<ArrayList<QBUser>> {
                                    override fun onSuccess(
                                        qbUsersList: ArrayList<QBUser>?,
                                        p1: Bundle?
                                    ) {
                                        if (qbUsersList != null) {
                                            qbDialogUsers = ArrayList()
                                            qbDialogUserIds = ArrayList()

                                            for (user in qbUsersList) {
                                                QbUsersDbManager.saveUser(user)
                                                if (user.id != currentUser?.id) {
                                                    qbDialogUsers.add(user)
                                                    qbDialogUserIds.add(user.id)
                                                }
                                            }
                                        }
                                        binding.progressBar.visibility = View.GONE
                                    }

                                    override fun onError(p1: QBResponseException?) {
                                        p1?.printStackTrace()
                                        binding.progressBar.visibility = View.GONE


                                    }
                                })
                        }
                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onError(p0: QBResponseException?) {
                        binding.progressBar.visibility = View.GONE

                    }
                })
        } else {
            binding.progressBar.visibility = View.GONE
        }

    }

    /**
     * Show text for repeat task string
     */
    private fun setRepeatText(
        repeatType: String?,
        repeatValue: String?,
        endType: String?,
        endValue: String?
    ) {
        var repeatString = ""

        when (repeatType) {
            "day" -> {
                repeatString = "Repeats every day"
            }

            "month" -> {
                repeatString = "Repeats monthly"
                dateValue = repeatValue!!
            }

            "year" -> {
                repeatString = "Repeats yearly"
                val tempArray = repeatValue?.split("-")?.toTypedArray()
                dateValue = tempArray?.get(0).toString()
                monthValue = tempArray?.get(1).toString()
            }

            else -> {
                val weekNumberList = repeatValue?.split(",")?.toTypedArray()
                weekList.clear()
                if (weekNumberList != null) {
                    weekList = weekNumberList.toCollection(ArrayList())
                }
                val weekNames = ArrayList<String>()

                if (weekNumberList != null) {
                    if ("1" in weekNumberList) weekNames.add("Mon")
                    if ("2" in weekNumberList) weekNames.add("Tue")
                    if ("3" in weekNumberList) weekNames.add("Wed")
                    if ("4" in weekNumberList) weekNames.add("Thu")
                    if ("5" in weekNumberList) weekNames.add("Fri")
                    if ("6" in weekNumberList) weekNames.add("Sat")
                    if ("7" in weekNumberList) weekNames.add("Sun")
                }
                repeatString = "Repeats weekly on " + TextUtils.join(", ", weekNames)
            }
        }

        if (endType != "never") {
            if (endType == "times") repeatString += if (endValue == "1") "; for 1 time" else "; for $endValue times"
            else if (endType == "date") {
                repeatString += "; until $endValue"
            }
        }

       // binding.txtRepeat.text = repeatString
    }

    /**
     * Create team/group
     */
//    private fun createTeam() {
//        val tempArrayList = ArrayList<Int>()
//        tempArrayList.addAll(participantsIds)
//        val qbUsersList = QbUsersDbManager.getQbUsersByExternalIds(tempArrayList)
//        createDialog(qbUsersList, binding.taskTitle.text.toString(), createTaskVM.createTaskRequestModel.image_url)
//    }

//    private fun createDialog(selectedUsers: ArrayList<QBUser>, chatName: String, teamImage: String?) {
//
//        val groupImage = if (teamImage.isNullOrBlank()) "" else teamImage
//
//        ChatHelper.createDialogWithSelectedUsers(selectedUsers, chatName, groupImage, binding.description.text.toString(), false,
//            object : QBEntityCallback<QBChatDialog> {
//                override fun onSuccess(dialog: QBChatDialog, args: Bundle?) {
//                    Log.d(TAG, "Creating Dialog Successful")
//
//                    qbChatDialog = dialog
//                    createTaskVM.createTaskRequestModel.qb_team_id = dialog.dialogId
//                    createTaskVM.callCreateTaskAPI()
//
//                }
//
//                override fun onError(error: QBResponseException) {
//                    Log.d(TAG, "Creating Dialog Error: " + error.message)
//                    showMessage(getString(R.string.dialogs_creation_error))
//                }
//            }
//        )
//    }

    private inner class SystemMessagesListener : QBSystemMessageListener {
        override fun processMessage(qbChatMessage: QBChatMessage) {
            dialogsManager.onSystemMessageReceived(qbChatMessage)
        }

        override fun processError(e: QBChatException, qbChatMessage: QBChatMessage) {

        }
    }

    private fun unregisterQbChatListeners() {
        if (this::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        dialogsManager.removeManagingDialogsCallbackListener(this)
    }

    /**
     * Edit Group/Team
     */
//    private fun editDialog(qbUsers: ArrayList<QBUser>, chatName: String, teamImage: String?) {
//        binding.progressBar.visibility = View.VISIBLE
//
//        val groupName = if (chatName.isBlank()) "" else chatName
//        val groupImage = if (teamImage.isNullOrBlank()) "" else teamImage
//        val groupDesc = if (binding.description.text.toString().isBlank()) "" else binding.description.text.toString()
//
//        if (qbChatDialog != null && currentUser != null && qbChatDialog?.dialogId != null) {
//            val existingOccupants = qbChatDialog?.occupants
//            val newUserIds = ArrayList<Int>()
//            val oldUserIds = ArrayList<Int>()
//
//            if (existingOccupants != null) {
//                for (user in qbUsers) {
//                    if (!existingOccupants.contains(user.id)) {
//                        newUserIds.add(user.id!!)
//                    }
//                }
//                for (id in existingOccupants) {
//                    if (!qbDialogUserIds.contains(id) && id != currentUser?.id) {
//                        oldUserIds.add(id)
//                    }
//                }
//
//                ChatHelper.getDialogById(qbChatDialog!!.dialogId, object : QBEntityCallback<QBChatDialog> {
//                    override fun onSuccess(qbChatDialog: QBChatDialog, p1: Bundle?) {
//                        dialogsManager.sendMessageRemovedUsers(qbChatDialog, oldUserIds)
//                        dialogsManager.sendMessageAddedUsers(qbChatDialog, newUserIds)
//
//                        dialogsManager.sendSystemMessageRemovedUser(systemMessagesManager, qbChatDialog, oldUserIds)
//                        dialogsManager.sendSystemMessageAddedUser(systemMessagesManager, qbChatDialog, newUserIds)
//
//                        qbChatDialog.let {
//                            this@CreateTaskActivity.qbChatDialog = it
//                        }
//                        updateDialog(qbUsers, groupName, groupImage, groupDesc)
//                    }
//
//                    override fun onError(e: QBResponseException?) {
//                        binding.progressBar.visibility = View.GONE
//                        goBack()
//                    }
//                })
//            } else {
//               goBack()
//            }
//        } else {
//            goBack()
//            binding.progressBar.visibility = View.GONE
//        }
//    }


//    private fun updateDialog(selectedUsers: ArrayList<QBUser>, groupName: String?, groupImage: String?, groupDesc: String?) {
//        ChatHelper.updateDialog(qbChatDialog!!, selectedUsers, groupName, groupImage, groupDesc,
//            object : QBEntityCallback<QBChatDialog> {
//                override fun onSuccess(dialog: QBChatDialog, args: Bundle?) {
//                    binding.progressBar.visibility = View.GONE
//                    goBack()
//                }
//
//                override fun onError(e: QBResponseException) {
//                    binding.progressBar.visibility = View.GONE
//                    e.printStackTrace()
//                    goBack()
//                }
//            })
//    }

//    private fun updateDialogWithTask(){
//        binding.progressBar.visibility = View.VISIBLE
//        createTaskVM.setMessage(Constants.SHOW_PROGRESS)
//        if(createTaskVM.taskId != null && qbChatDialog != null && createTaskVM.createTaskRequestModel.is_start_team == true){
//            ChatHelper.addTaskToDialog(qbChatDialog!!, createTaskVM.taskId!!, object: QBEntityCallback<QBChatDialog> {
//                override fun onSuccess(p0: QBChatDialog?, p1: Bundle?) {
//                    binding.progressBar.visibility = View.GONE
//                    finish()
//                    startActivity(intent)
//                }
//
//                override fun onError(p0: QBResponseException?) {
//                    p0?.printStackTrace()
//                    binding.progressBar.visibility = View.GONE
//                    finish()
//                    startActivity(intent)
//                }
//            })
//        }
//        else{
//            finish()
//            startActivity(intent)
//        }
//    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {

    }

    override fun onDialogUpdated(chatDialog: String) {

    }

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {

    }

    fun goBack() {
        finish()
    }

    override fun onConfirm(place: String) {

    }

}