package com.example.sameteam.homeScreen.bottomNavigation.taskModule.bottomSheet

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.sameteam.BR
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.BottomSheetTaskDetailsBinding
import com.example.sameteam.fcm.sendPushMessage
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskStatusRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CallActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.ChatActivity
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.TaskFragment
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantsModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetEventRequestModel
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.service.LoginService
import com.example.sameteam.quickBlox.util.*
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBDialogType
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser
import com.quickblox.videochat.webrtc.QBRTCClient
import com.quickblox.videochat.webrtc.QBRTCTypes


class TaskDetailsBottomSheet(
    val listener: OnBottomSheetDismissListener,
    val mContext: Context,
    val taskId: Int,
) : BottomSheetDialogFragment(),
    OnResponseListener, ConfirmDialog.ConfirmClickListener {
    private val TAG = "TasksDetailsBottomSheet"

    lateinit var binding: BottomSheetTaskDetailsBinding
    lateinit var popupWindow: PopupWindow

    var users = ArrayList<TaskParticipantsModel>()

    private var messageString = MutableLiveData<Event<String>>()
    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString
    lateinit var model: TaskDetailsResponseModel.Data
    var isAccepted = false
    var isDeclined = false
    var isUserParticipant = false
    private var doCallback = false
    lateinit var taskParticipant: TaskParticipantsModel
    var qbChatDialog: QBChatDialog? = null

    var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    var callUsersList: ArrayList<QBUser> = ArrayList()
    var acceptedUsersList: ArrayList<Int> = ArrayList()
    private var currentUser: QBUser? = null
    private var firstTimeLoaded = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_task_details, container, false)

        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet: View? =
                d.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val sheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            sheetBehavior.peekHeight = bottomSheet.height
        }

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Handle the dismiss event here
        if (doCallback) {
            listener.onBottomSheetDismissed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setPopupWindow()

        if (SharedPrefsHelper.hasQbUser())
            currentUser = SharedPrefsHelper.getQbUser()
        else {
            shortToast(getString(R.string.something_went_wrong))
            dismiss()
        }

        callGetTaskDetails()

        /**
         * Based on TaskDetailsBottomSheet location, Observe confirmation in activity and call APIs
         */
        if (mContext is HomeActivity) {
            (activity as HomeActivity).homeVM.onConfirm().observe(this, Observer { event ->
                event?.getContentIfNotHandled()?.let {
                    when (it) {
                        "Delete" -> {
                            deleteChatDialog()
                        }

                        "Complete" -> {
                            callChangeStatusAPI()
                        }
                    }
                }
            })
        }

        binding.btnPopupMenu.setOnClickListener {
            popupWindow.showAsDropDown(it, 0, -20, Gravity.END)
        }

        binding.btnAccept.setOnClickListener {
            if (isAccepted) {
                Toast.makeText(requireContext(), "Response already recorded", Toast.LENGTH_SHORT)
                    .show()
            } else {
                callParticipantsResponseAPI(Constants.ACCEPTED)
                doCallback = true
            }
        }

        binding.btnDecline.setOnClickListener {
            if (isDeclined) {
                Toast.makeText(requireContext(), "Response already recorded", Toast.LENGTH_SHORT)
                    .show()
            } else {
                callParticipantsResponseAPI(Constants.DECLINED)
                doCallback = true
            }
        }

        binding.btnChat.setOnClickListener {
            if (qbChatDialog != null) {
                val intent = Intent(requireActivity(), ChatActivity::class.java)
                val gson = Gson()
                intent.putExtra("chatDialog", gson.toJson(qbChatDialog))
                startActivity(intent)
            } else {
                val confirmDialog =
                    ConfirmDialog(
                        requireContext(),
                        "Chat group not found!",
                        "AlreadyCompleted"
                    )

                confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")
            }
        }

        binding.btnAudioCall.setOnClickListener {
            if (currentUser != null) {

                checkAudioPermission()
            }
        }

        binding.btnVideoCall.setOnClickListener {
            if (currentUser != null) {
                checkAudioVideoPermissions()
            }
        }

        binding.btnSignup.setOnClickListener {
            signUpForTask(taskId)
        }

        /**
         * General Observer
         */
        observedChanges().observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                        if (firstTimeLoaded)
                            binding.rootLayout.visibility = View.INVISIBLE
                    }

                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rootLayout.visibility = View.VISIBLE
                    }

                    Constants.SHOW_PROGRESS -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    Constants.HIDE_PROGRESS -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    Constants.NAVIGATE -> {
                        callGetTaskDetails()
                    }

                    else -> {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
    }

    /**
     * Inflate custom popup menu for edit,delete and change task status
     */
    private fun setPopupWindow() {
        val inflater =
            requireActivity().applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_menu_layout, null)

        val complete = view!!.findViewById(R.id.complete) as LinearLayout
        val edit = view.findViewById(R.id.edit) as LinearLayout
        val delete = view.findViewById(R.id.delete) as LinearLayout

        complete.visibility = View.GONE

        complete.setOnClickListener {

            if (model.completed == true) {
                val confirmDialog =
                    ConfirmDialog(
                        requireContext(),
                        "This task is already completed",
                        "AlreadyCompleted"
                    )

                confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")

            } else {
                val confirmDialog =
                    ConfirmDialog(
                        requireContext(),
                        "Are you sure you want to change the status of the task for all participants? This action cannot be undone.",
                        "Complete"
                    )
                confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")
            }

        }

        delete.setOnClickListener {
            val confirmDialog =
                ConfirmDialog(
                    requireContext(),
                    "Are you sure you want to delete the task for all participants? This action cannot be undone",
                    "Delete"
                )
            confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")

            confirmDialog.setConfirmClickListener(this@TaskDetailsBottomSheet)

        }

        edit.setOnClickListener {
            if (model.completed == true) {
                val confirmDialog =
                    ConfirmDialog(
                        requireContext(),
                        "This task is already completed",
                        "AlreadyCompleted"
                    )

                confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")
            } else {
                val intent = Intent(context, CreateTaskActivity::class.java)
                val gson = Gson()
                intent.putExtra("taskDetails", gson.toJson(model))
                context?.startActivity(intent)
                dismiss()
            }

        }

        popupWindow = PopupWindow(view, 400, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
    }

    private fun deleteChatDialog() {
        setMessage(Constants.SHOW_PROGRESS)

        if (qbChatDialog != null) {
            ChatHelper.deleteDialog(qbChatDialog!!, object : QBEntityCallback<Void> {
                override fun onSuccess(p0: Void?, p1: Bundle?) {
                    callDeleteTaskAPI()
                }

                override fun onError(p0: QBResponseException?) {
                    setMessage(Constants.HIDE_PROGRESS)
                    longToast(R.string.something_went_wrong)
                    p0?.printStackTrace()
                }
            })
        } else {
            callDeleteTaskAPI()
        }
    }


    private fun callGetTaskDetails() {
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callGetTaskById(this, taskId)
    }

    private fun callDeleteTaskAPI() {
        APITask.getInstance().callDeleteTask(this, taskId)
    }

    private fun callChangeStatusAPI() {
        setMessage(Constants.SHOW_PROGRESS)
        val statusModel = TaskStatusRequestModel(taskId)
        APITask.getInstance().callChangeTaskStatus(this, statusModel)
    }

    private fun callParticipantsResponseAPI(value: String) {
        val hashmap = HashMap<String, String>()

        if (this::taskParticipant.isInitialized) {
            setMessage(Constants.SHOW_PROGRESS)
            hashmap["id"] = taskParticipant.id.toString()
            hashmap["response"] = value
            APITask.getInstance().callParticipantResponse(this, hashmap)
        }
    }

    private fun signUpForTask(taskId: Int) {

        setMessage(Constants.SHOW_PROGRESS)

        val requestModel = GetEventRequestModel()
        requestModel.id = taskId

        APITask.getInstance().signUpForTask(this, requestModel)

    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {

        if (requestCode == 1) {  // Response of getTaskByID API
            model = (response as TaskDetailsResponseModel).data
            Log.d(TAG, "onResponseReceived: ${Gson().toJson(model)}")

            firstTimeLoaded = false

            if (context != null)
                fillTaskDetails(model)

            if (!model.qb_team_id.isNullOrBlank()) {
                ChatHelper.getDialogById(
                    model.qb_team_id!!,
                    object : QBEntityCallback<QBChatDialog> {
                        override fun onSuccess(p0: QBChatDialog?, p1: Bundle?) {
                            qbChatDialog = p0

                            callUsersList.clear()

                            if (p0 != null) {
                                ChatHelper.getUsersFromDialog(
                                    p0,
                                    object : QBEntityCallback<ArrayList<QBUser>> {
                                        override fun onSuccess(
                                            qbUsersList: ArrayList<QBUser>?,
                                            p1: Bundle?
                                        ) {
                                            if (qbUsersList != null) {
                                                for (user in qbUsersList) {
                                                    QbUsersDbManager.saveUser(user)
                                                    if (user.id != currentUser?.id && acceptedUsersList.contains(
                                                            user.externalId.toInt()
                                                        )
                                                    )
                                                        callUsersList.add(user)
                                                }
                                            }
                                            setMessage(Constants.HIDE)
                                        }

                                        override fun onError(p1: QBResponseException?) {
                                            p1?.printStackTrace()
                                            setMessage(Constants.HIDE)
                                        }
                                    })
                            }
                            setMessage(Constants.HIDE)
                        }

                        override fun onError(p0: QBResponseException?) {
                            setMessage(Constants.HIDE)

                        }

                    })
            } else {
                setMessage(Constants.HIDE)
            }

        } else if (requestCode == 2) {  // Response of Delete Task API
            setMessage(Constants.HIDE_PROGRESS)
            Toast.makeText(context, (response as CommonModel).message, Toast.LENGTH_LONG)
                .show()
            val intent = Intent(requireContext(), HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("taskDeleted", true)
            startActivity(intent)
            dismiss()
        } else if (requestCode == 3) {  // Response of ChangeTaskStatus API
            setMessage(Constants.HIDE_PROGRESS)
            model = (response as TaskDetailsResponseModel).data
            Toast.makeText(
                context,
                (response as TaskDetailsResponseModel).message,
                Toast.LENGTH_LONG
            )
                .show()
        } else if (requestCode == 4) {  // Response of ParticipantsResponse API
            setMessage(Constants.HIDE_PROGRESS)
            Toast.makeText(
                context,
                (response as TaskParticipantResponseModel).message,
                Toast.LENGTH_LONG
            )
                .show()

            taskParticipant = (response as TaskParticipantResponseModel).data

            if (taskParticipant.response == Constants.ACCEPTED && isUserParticipant) {
                setSelected(binding.btnAccept, binding.btnDecline)
                binding.btnAccept.text = "Accepted"
                isAccepted = true
                isDeclined = false
            } else if (taskParticipant.response == Constants.DECLINED) {
                setSelected(binding.btnDecline, binding.btnAccept)
                binding.btnDecline.text = "Declined"
                isAccepted = false
                isDeclined = true
            } else {
                setUnselected(binding.btnAccept, binding.btnDecline)
                isAccepted = false
                isDeclined = false
            }

            for (user in users) {
                if (user.id == taskParticipant.id) {
                    user.response = taskParticipant.response
                }
            }

            binding.recView.adapter?.notifyDataSetChanged()

            binding.contactLayout.visibility =
                if (isUserParticipant && isAccepted) View.VISIBLE else View.GONE

        } else if (requestCode == 5) {
            setMessage(Constants.HIDE_PROGRESS)

            setMessage(Constants.NAVIGATE)
        }

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        when (requestCode) {
            1 -> {
                setMessage(Constants.HIDE)
                Log.d(TAG, "Response TaskDetails API Error : Code $responseCode  Message $message")
                setMessage(message)
                dismiss()
            }

            2 -> {
                setMessage(Constants.HIDE_PROGRESS)
                Log.d(TAG, "Response Delete Task API Error : Code $responseCode  Message $message")
                setMessage(message)
            }

            3 -> {
                setMessage(Constants.HIDE_PROGRESS)
                Log.d(TAG, "Response Change Task API Error : Code $responseCode  Message $message")
                setMessage(message)
            }

            4 -> {
                setMessage(Constants.HIDE_PROGRESS)
                Log.d(
                    TAG,
                    "Task participants response API Error : Code $responseCode  Message $message"
                )
                setMessage(message)
            }

            5 -> {
                setMessage(Constants.HIDE_PROGRESS)
                setMessage(message)
            }
        }
    }

    private fun fillTaskDetails(model: TaskDetailsResponseModel.Data) {
        val loggedUser = SharedPrefs.getUser(requireContext())

        /**
         * Load users from API
         */
        if (!model.task_participants.isNullOrEmpty()) {
            users = ArrayList(model.task_participants.filter { item -> item.user != null })
            binding.recView.adapter = BindingAdapter(
                layoutId = R.layout.participants_event_details_card,
                br = BR.model,
                list = users,
                clickListener = { view, position ->
                    Log.i("On Tap", "yes")
                    openParticipantBottomSheet()
                }
            )

            binding.participantLayout.setOnClickListener {
                Log.i("On Tap", "yes")
                openParticipantBottomSheet()
            }
        }

        binding.taskName.text = model.name


        if (model.team_name != null) {
            binding.teamNames.text = "Teams: ${model.team_name}"
            binding.teamNames.visibility = View.VISIBLE
        } else {
            binding.teamNames.visibility = View.GONE
        }

        if (model.user.first_name != null && model.user.last_name != null) {
            binding.textActivityCreator.text = model.user.first_name + " " + model.user.last_name
        }

        if (!model.description.isNullOrBlank()) binding.description.text = model.description
        else binding.description.visibility = View.GONE

        if (model.event != null) binding.eventName.text = model.event?.title
        else binding.eventName.visibility = View.GONE

        if (!model.location.isNullOrBlank()) binding.location.text = model.location
        else binding.locationLayout.visibility = View.GONE


        if (model.image_url.isNullOrBlank()) {
            Glide.with(requireActivity())
                .asDrawable()
                .load(R.drawable.task_image_placeholder)
                .placeholder(R.drawable.task_image_placeholder)
                .into(binding.taskImage)
        } else {
            Glide.with(requireActivity())
                .load(model.image_url)
                .placeholder(R.drawable.task_image_placeholder)
                .error(R.drawable.task_image_placeholder)
                .into(binding.taskImage)
        }

        binding.participantLayout.visibility =
            if (model.task_participants.isEmpty()) View.GONE else View.VISIBLE

        val startLocalTimestamp = utcTimestampToLocalDateTime(model.start_time_stamp.toString())
        val startDate = startLocalTimestamp?.toLocalDate()?.let { getFormattedDate(it) }
        if (model.all_day == true) {
            binding.time.text = "$startDate, ${context?.getString(R.string.all_day)}"
        } else {
            val endLocalTimestamp = utcTimestampToLocalDateTime(model.end_time_stamp.toString())

            if (startLocalTimestamp != null && endLocalTimestamp != null) {
                val endDate = endLocalTimestamp.toLocalDate()?.let { getFormattedDate(it) }

                if (startDate == endDate) {
                    val timeString =
                        startDate + ", ${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))} - ${
                            getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))
                        }"
                    binding.time.text = timeString
                } else {
                    val startTimeString =
                        startDate + ", ${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))}"
                    val endTimeString =
                        endDate + ", ${getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))}"
                    binding.time.text = "$startTimeString - $endTimeString"
                }
            }
        }


        var txtTime = ""
        val unitMap = hashMapOf("m" to "minutes", "h" to "hours", "d" to "day")

        for (item in model.remind_me) {
            if (item.value == "1" && item.unit == "h") txtTime += "1 hour, "
            else {
                txtTime += "${item.value} ${unitMap.getValue(item.unit.toString())}, "
            }
        }

        if (txtTime.isNotBlank())
            binding.txtReminder.text = "${txtTime.subSequence(0, txtTime.length - 2)} before"
        else
            binding.txtReminder.text = requireActivity().getString(R.string.dont_remind)


//        if (model.repeat_task == false) binding.txtRepeat.text =
//            getString(R.string.does_not_repeat)
//        else setRepeatText(
//            model.repeat_type,
//            model.repeat_value,
//            model.repeat_end_type,
//            model.repeat_end_value
//        )

        if (model.total_slots == 0 || model.total_slots == null) {
            binding.slotLayout.visibility = View.GONE
        } else {
            binding.slotLayout.visibility = View.VISIBLE
            binding.txtSlots.text =
                "${model.available_slots} of ${model.total_slots} Slots Available"
        }

        for (item in model.task_participants) {

            if (item.user?.id != null) {

                if (item.response == Constants.ACCEPTED)
                    acceptedUsersList.add(item.user?.id!!)

                if (loggedUser?.id == item.user?.id) {

                    taskParticipant = item
                    isUserParticipant = true

                    when (item.response) {
                        Constants.ACCEPTED -> {
                            setSelected(binding.btnAccept, binding.btnDecline)
                            binding.btnAccept.text = "Accepted"
                            binding.btnDecline.text = requireContext().getString(R.string.decline)
                            isAccepted = true
                            isDeclined = false
                        }

                        Constants.DECLINED -> {
                            setSelected(binding.btnDecline, binding.btnAccept)
                            binding.btnDecline.text = "Declined"
                            binding.btnAccept.text = requireContext().getString(R.string.accept)
                            isAccepted = false
                            isDeclined = true
                        }

                        else -> {
                            setUnselected(binding.btnAccept, binding.btnDecline)
                            binding.btnAccept.text = requireContext().getString(R.string.accept)
                            binding.btnDecline.text = requireContext().getString(R.string.decline)
                            isAccepted = false
                            isDeclined = false
                        }
                    }
                }
            }
        }

        Log.d("isUserParticipant", isUserParticipant.toString())
        binding.invitationLayout.visibility =
            if (isUserParticipant && model.sign_up_user == false) View.VISIBLE else View.GONE

        binding.contactLayout.visibility =
            if (isUserParticipant && isAccepted && !model.qb_team_id.isNullOrBlank()) View.VISIBLE else View.GONE

        if (model.user.id == loggedUser?.id) {
            // binding.invitationLayout.visibility = View.GONE
            binding.btnPopupMenu.visibility = View.VISIBLE
        } else {
            binding.btnPopupMenu.visibility = View.GONE
        }

        binding.btnSignup.visibility = if (model.slot_available == true) View.VISIBLE else View.GONE

    }

    private fun openParticipantBottomSheet() {

        val fragment = ParticipantsBottomSheet(mContext, model.task_participants)
        fragment.show(
            childFragmentManager,
            ParticipantsBottomSheet::class.java.name
        )
    }

    private fun getProperString(text: String): String {
        val array = text.split(":").toTypedArray()
        val hours = array[0].toInt()
        return "$hours:${array[1]}"
    }

    private fun setSelected(textView: TextView, textView2: TextView) {
        textView.background.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_demibold)

        textView2.background.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        textView2.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        textView2.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_regular)
    }

    fun setUnselected(textView: TextView, textView2: TextView) {
        textView.background.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_regular)

        textView2.background.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        textView2.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        textView2.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_regular)
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
            }

            "year" -> {
                repeatString = "Repeats yearly"
            }

            else -> {
                val weekNumberList = repeatValue?.split(",")?.toTypedArray()
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

    private fun checkAudioPermission() {
        Dexter.withContext(requireActivity())
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (checkIsLoggedInChat()) {
                        if (qbChatDialog != null)
                            startCall(false)
                        else
                            loadUsersForCall(false)

                    }
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts(
                        "package",
                        requireActivity().packageName,
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
        Dexter.withContext(requireActivity())
            .withPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if (p0?.areAllPermissionsGranted() == true) {
                        if (checkIsLoggedInChat()) {
                            if (qbChatDialog != null)
                                startCall(true)
                            else
                                loadUsersForCall(true)
                        }
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(
                            "package",
                            requireActivity().packageName,
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

    private fun loadUsersForCall(isVideoCall: Boolean) {
        callUsersList.clear()
        callUsersList = QbUsersDbManager.getQbUsersByExternalIds(acceptedUsersList)

        if (callUsersList.contains(currentUser))
            callUsersList.remove(currentUser)

        startCall(isVideoCall)
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
                    getString(R.string.error_max_opponents_count),
                    Constants.MAX_OPPONENTS_COUNT
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

        var groupName = ""
        groupName = if (qbChatDialog != null && qbChatDialog!!.type != QBDialogType.PRIVATE)
            qbChatDialog?.name.toString()
        else
            binding.taskName.text.toString()

        sendPushMessage(
            opponentsList,
            currentUser?.fullName!!,
            newSessionID,
            opponentsIDsString,
            opponentNamesString,
            isVideoCall,
            groupName
        )

        CallActivity.start(requireContext(), false, groupName)
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

    override fun onConfirm(place: String) {
        callDeleteTaskAPI()
    }

}