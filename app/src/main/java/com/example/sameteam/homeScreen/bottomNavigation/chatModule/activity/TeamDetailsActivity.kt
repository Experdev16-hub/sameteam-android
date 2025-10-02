package com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.sameteam.BR
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.ActivityTeamDetailsBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.viewModel.TeamDetailsVM
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.OverlapImageModel
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.managers.DialogsManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.quickBlox.util.longToast
import com.example.sameteam.quickBlox.util.shortToast
import com.example.sameteam.widget.ConfirmDialog
import com.google.gson.Gson
import com.quickblox.auth.session.QBSessionManager
import com.quickblox.chat.QBChatService
import com.quickblox.chat.QBSystemMessagesManager
import com.quickblox.chat.exception.QBChatException
import com.quickblox.chat.listeners.QBSystemMessageListener
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBChatMessage
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.users.model.QBUser

class TeamDetailsActivity : BaseActivity<ActivityTeamDetailsBinding>(), ConfirmDialog.ConfirmClickListener,
    DialogsManager.ManagingDialogsCallbacks {
    private val TAG = "TeamDetailsActivity"

    override fun layoutID() = R.layout.activity_team_details

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(TeamDetailsVM::class.java)

    lateinit var binding: ActivityTeamDetailsBinding
    lateinit var teamDetailsVM: TeamDetailsVM
    lateinit var popupWindow: PopupWindow

    var qbChatDialog: QBChatDialog? = null
    var qbDialogUsers : ArrayList<QBUser>? = null
    var userModelList : ArrayList<UserModel> = ArrayList()
    var adminList : ArrayList<UserModel> = ArrayList()
    var qbDialogAdmin : Int? = null
    var currentUser : QBUser? = null
    var qbDialogImages : ArrayList<OverlapImageModel> = ArrayList()
    var hasTask: Boolean? = false
    var isAdmin: Boolean?  = false
    var isEdit = false
    var taskDetails: TaskDetailsResponseModel.Data? = null
    var taskId: Int? = null


    private var dialogsManager: DialogsManager = DialogsManager()
    private var systemMessagesListener: SystemMessagesListener = SystemMessagesListener()
    private lateinit var systemMessagesManager: QBSystemMessagesManager

    private inner class SystemMessagesListener : QBSystemMessageListener {
        override fun processMessage(qbChatMessage: QBChatMessage) {
            Log.d(TAG, "System Message Received: " + qbChatMessage.id)
            dialogsManager.onSystemMessageReceived(qbChatMessage)
        }

        override fun processError(e: QBChatException?, qbChatMessage: QBChatMessage?) {
            Log.d(TAG, "System Messages Error: " + e?.message + "With MessageID: " + qbChatMessage?.id)
        }
    }

    override fun onResume() {
        super.onResume()

        if(qbChatDialog != null && isEdit){
            hasTask = false
            isAdmin  = false
            isEdit = false
            userModelList.clear()
            qbDialogImages.clear()
            adminList.clear()
            qbDialogUsers?.clear()
            getChatDialog(qbChatDialog?.dialogId!!)
        }
    }

    override fun initActivity(mBinding: ViewDataBinding) {
        binding = mBinding as ActivityTeamDetailsBinding
        teamDetailsVM = getViewModel() as TeamDetailsVM

        binding.customToolbar.rightIcon.visibility = View.GONE
        binding.customToolbar.leftIcon.setOnClickListener {
            onBackPressed()
        }
        binding.customToolbar.title.text = "Team Details"

        binding.progressBar.visibility = View.VISIBLE
        binding.rootLayout.visibility = View.INVISIBLE

        if(SharedPrefsHelper.hasQbUser())
            currentUser = SharedPrefsHelper.getQbUser()
        else {
            showMessage(getString(R.string.something_went_wrong))
            finish()
        }

        if (QBChatService.getInstance() != null) {
            systemMessagesManager = QBChatService.getInstance().systemMessagesManager
            systemMessagesManager.addSystemMessageListener(systemMessagesListener)
        }

        try{
            val gson = Gson()
            val gsonString = intent.getStringExtra("chatDialog")
            if(!gsonString.isNullOrBlank()){
                val dialog = gson.fromJson(gsonString, QBChatDialog::class.java)
                getChatDialog(dialog.dialogId)
            }
            else{
                shortToast(R.string.something_went_wrong)
                finish()
            }
        }
        catch (e : Exception){
            e.printStackTrace()
            shortToast(R.string.something_went_wrong)
            finish()
        }

        binding.btnPopupMenu.setOnClickListener {
            popupWindow.showAsDropDown(it, 0, -10, Gravity.END)
        }

        //Leave Team
        binding.btnSave.setOnClickListener {
            val confirmDialog =
                ConfirmDialog(
                    this,
                    "Are you sure you want to leave this team?",
                    "Leave"
                )
            confirmDialog.show(supportFragmentManager, "Confirm")
        }

        teamDetailsVM.observedChanges().observe(this, Observer { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    Constants.NAVIGATE -> {
                        val intent = Intent(this@TeamDetailsActivity, HomeActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.putExtra("deleted", true)
                        startActivity(intent)
                    }
                    "setTaskDetails" -> {
                        taskDetails = teamDetailsVM.taskDetails
                    }
                    else -> {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        })
    }

    private fun getChatDialog(dialogId: String){
        binding.progressBar.visibility = View.VISIBLE

        ChatHelper.getDialogById(dialogId, object : QBEntityCallback<QBChatDialog>{
            override fun onSuccess(p0: QBChatDialog?, p1: Bundle?) {
                qbChatDialog = p0
                if(qbChatDialog != null)
                    loadDetails()
            }

            override fun onError(p0: QBResponseException?) {
                shortToast(R.string.something_went_wrong)
                finish()
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterQbChatListeners()
    }

    private fun unregisterQbChatListeners() {
        if (this::systemMessagesManager.isInitialized)
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener)

        dialogsManager.removeManagingDialogsCallbackListener(this)
    }


    fun loadDetails(){
        Log.d(TAG, "loadDetails: Called")
        if(!qbChatDialog?.photo.isNullOrBlank()){
            Glide.with(this)
                .load(qbChatDialog?.photo)
                .placeholder(R.drawable.task_image_placeholder)
                .error(R.drawable.task_image_placeholder)
                .into(binding.imageView)
        }

        binding.imageView.setOnClickListener {
            if(!qbChatDialog?.photo.isNullOrBlank()){
                val intent = Intent(this, ShowImageActivity::class.java)
                intent.putExtra("imageUrl", qbChatDialog?.photo)
                startActivity(intent)
            }
        }

        binding.teamName.text = qbChatDialog?.name


        val customData = qbChatDialog?.customData
        if(customData != null){

            /**
             * Get group description from Dialog Custom Data
             */
            val desc = customData.getString("description")
            if(!desc.isNullOrBlank()) {
                binding.teamDescription.visibility = View.VISIBLE
                binding.teamDescription.text = desc
            } else {
                binding.teamDescription.visibility = View.GONE
            }


            /**
             * Get group images from Dialog Custom Data
             */
            val imageUrls: List<String>? = customData.getArray("imageUrls")
            val sessionManager = QBSessionManager.getInstance()
            if(imageUrls != null && sessionManager != null && !sessionManager.token.isNullOrBlank()){
                binding.imagesLayout.visibility = View.VISIBLE
                for(url in imageUrls){
                    val newUrl = url + "?token=${sessionManager.token}"
                    val tempModel = OverlapImageModel(newUrl)
                    qbDialogImages.add(tempModel)
                }
                qbDialogImages.reverse()
                loadImagesList()
            }
            else{
               binding.imagesLayout.visibility = View.GONE
            }


            /**
             * Get Admin Details from Dialog custom Data
             */
            val adminId = customData.getInteger("admin")
            if(adminId != null){
                adminList.clear()
                qbDialogAdmin = adminId

                val tempUser = QbUsersDbManager.getUserById(adminId)
                if(tempUser != null && tempUser.customData != null){
                    val user = Utils.getUserFromQBUser(tempUser.customData)
                    adminList.add(user)
                }
            }

            /**
             * If team has task then load task details for edit & delete & Complete task
             */
//            hasTask = customData.getBoolean("hasTask")
//            taskId = customData.getInteger("taskId")
//            if(hasTask != null && taskId != null && taskId != 0){
//                teamDetailsVM.getTaskById(taskId!!)
//            }
//            else hasTask = false


            /**
             *  Show popup menu to Admin only
             */
            if(currentUser != null && adminId != null && currentUser?.id == adminId) {
                binding.btnPopupMenu.visibility = View.VISIBLE
                isAdmin = true
                setPopupWindow(hasTask!!)
            }
            else {
                isAdmin = false
                binding.btnPopupMenu.visibility = View.GONE
            }

        }
        else{
            binding.btnPopupMenu.visibility = View.GONE
            binding.imagesLayout.visibility = View.GONE
            binding.teamDescription.visibility = View.GONE
        }

        getUsersFromDialog()

    }


    /**
     * Inflate custom popup menu for edit,delete team
     */
    fun setPopupWindow(hasTask:Boolean) {
        val inflater = applicationContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.popup_menu_layout,null)

        val complete = view!!.findViewById(R.id.complete) as LinearLayout
        val edit = view.findViewById(R.id.edit) as LinearLayout
        val delete = view.findViewById(R.id.delete) as LinearLayout

        complete.visibility = View.GONE
        val param = edit.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0,0,0,0)
        edit.layoutParams = param
//        if(hasTask){
//            complete.visibility = View.VISIBLE
//        }
//        else{
//            complete.visibility = View.GONE
//            val param = edit.layoutParams as ViewGroup.MarginLayoutParams
//            param.setMargins(0,0,0,0)
//            edit.layoutParams = param
//        }


//        complete.setOnClickListener {
//            if(taskDetails != null){
//                if(taskDetails?.completed == true ){
//                    val confirmDialog =
//                        ConfirmDialog(this, "This task is already completed", "AlreadyCompleted")
//
//                    confirmDialog.show(supportFragmentManager, "Confirm")
//                }
//                else{
//                    val confirmDialog =
//                        ConfirmDialog(this, "Are you sure you want to change the status of the task for all participants? This action cannot be undone.", "Complete")
//                    confirmDialog.show(supportFragmentManager, "Confirm")
//                }
//            }
//
//        }

        delete.setOnClickListener {
            val confirmDialog =
                ConfirmDialog(this, "Are you sure you want to delete this team?", "Delete")
            confirmDialog.show(supportFragmentManager, "Confirm")
//            if(hasTask){
//                val confirmDialog =
//                    ConfirmDialog(this, "Are you sure you want to delete this task and team?", "Delete")
//                confirmDialog.show(supportFragmentManager, "Confirm")
//            }
//            else{
//                val confirmDialog =
//                    ConfirmDialog(this, "Are you sure you want to delete this team?", "Delete")
//                confirmDialog.show(supportFragmentManager, "Confirm")
//            }

        }

        edit.setOnClickListener {

//            if(hasTask){
//                if(taskDetails != null){
//                    if( taskDetails?.completed == true){
//                        val confirmDialog =
//                            ConfirmDialog(this, "This task is already completed", "AlreadyCompleted")
//
//                        confirmDialog.show(supportFragmentManager, "Confirm")
//                    }
//                    else{
//                        isEdit = true
//                        val intent = Intent(this, CreateTaskActivity::class.java)
//                        val gson = Gson()
//                        intent.putExtra("taskDetails", gson.toJson(taskDetails))
//                        startActivity(intent)
//                    }
//                }
//            }
//            else{
//                if(qbChatDialog != null){
//                    val intent = Intent(this,CreateTeamActivity::class.java)
//                    val gson = Gson()
//                    intent.putExtra("editChatDialog",gson.toJson(qbChatDialog))
//                    startActivity(intent)
//                }
//            }

            if(qbChatDialog != null){
                val intent = Intent(this,CreateTeamActivity::class.java)
                val gson = Gson()
                intent.putExtra("editChatDialog",gson.toJson(qbChatDialog))
                startActivity(intent)
            }
        }

        popupWindow = PopupWindow(view, 400, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
    }

    private fun getUsersFromDialog(){

        ChatHelper.getUsersFromDialog(qbChatDialog!!, object: QBEntityCallback<ArrayList<QBUser>>{
            override fun onSuccess(p0: ArrayList<QBUser>?, p1: Bundle?) {
                Log.d(TAG, "onSuccess: Root")
                qbDialogUsers = p0

                if(!qbDialogUsers.isNullOrEmpty()) {
                    loadUsersList()
                }
                binding.rootLayout.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }

            override fun onError(p0: QBResponseException?) {
                binding.progressBar.visibility = View.GONE
                binding.rootLayout.visibility = View.VISIBLE
                showMessage(getString(R.string.chat_load_users_error))
                p0?.printStackTrace()
            }
        })
    }

    /**
     *     load users of a group,
     *     Group admin can only edit the group details
     *     Group admin can only remove or add a person
     *     Group admin cannot leave the chat
     */
    fun loadUsersList(){
        var isCurrentUserInList = false

        for(user in qbDialogUsers!!){
            if(user.id == currentUser?.id)
                isCurrentUserInList = true
            if(!user.customData.isNullOrBlank()){
                val tempUser = Utils.getUserFromQBUser(user.customData)
                userModelList.add(tempUser)
            }
        }

        binding.btnSave.visibility = if(isCurrentUserInList && !isAdmin!! && !hasTask!!) View.VISIBLE else View.GONE

        binding.usersRecView.layoutManager = LinearLayoutManager(this)
        binding.usersRecView.adapter = UserDirectoryAdapter(this, userModelList, adminList, ArrayList())
        (binding.usersRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

    }

    /**
     * show all images for a group
     */
    private fun loadImagesList(){
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true)
        layoutManager.stackFromEnd = true
        binding.imageRecView.layoutManager = layoutManager
        qbDialogImages.forEach{ Log.d(TAG, "loadImagesList: ${it.imageUrl}")}
        binding.imageRecView.adapter = BindingAdapter(
            layoutId = R.layout.media_card_layout,
            br = BR.model,
            list = qbDialogImages,
            clickListener = { view, position ->
                when(view.id){
                    R.id.rootLayout -> {
                        val item = qbDialogImages[position]
                        if(!item.imageUrl.isNullOrBlank()){
                            val intent = Intent(this, ShowImageActivity::class.java)
                            intent.putExtra("imageUrl", item.imageUrl)
                            startActivity(intent)
                        }

                    }
                }
            }
        )
    }

    /**
     * When confirm clicked for delete dialog or Leaving group
     */
    override fun onConfirm(place: String) {
        if(qbChatDialog != null){


            if(place == "Delete"){
                binding.progressBar.visibility = View.VISIBLE

                ChatHelper.deleteDialog(qbChatDialog!!, object: QBEntityCallback<Void>{
                    override fun onSuccess(p0: Void?, p1: Bundle?) {

//                        if(hasTask == true && taskId != null){
//                            teamDetailsVM.callDeleteTaskAPI(taskId!!)
//                        }
//                        else{
//                            val intent = Intent(this@TeamDetailsActivity, HomeActivity::class.java)
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                            intent.putExtra("deleted", true)
//                            startActivity(intent)
//                            longToast("Team deleted successfully")
//                            binding.progressBar.visibility = View.GONE
//                        }

                        val intent = Intent(this@TeamDetailsActivity, HomeActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.putExtra("deleted", true)
                        startActivity(intent)
                        longToast("Team deleted successfully")
                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onError(p0: QBResponseException?) {
                        binding.progressBar.visibility = View.GONE
                        longToast(R.string.something_went_wrong)
                        p0?.printStackTrace()
                    }
                })
            }
            else if( place == "Leave") {
                binding.progressBar.visibility = View.VISIBLE

                dialogsManager.sendMessageLeftUser(qbChatDialog!!)
                dialogsManager.sendSystemMessageLeftUser(systemMessagesManager, qbChatDialog!!)
                try {
                    // Its a hack to give the Chat Server more time to process the message and deliver them
                    Thread.sleep(300)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }


                Log.d(TAG, "Leaving Dialog")
                ChatHelper.exitFromDialog(qbChatDialog!!, object : QBEntityCallback<QBChatDialog> {
                    override fun onSuccess(qbDialog: QBChatDialog, bundle: Bundle?) {
                        val intent = Intent(this@TeamDetailsActivity, HomeActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.putExtra("deleted", true)
                        startActivity(intent)
                        binding.progressBar.visibility = View.GONE
                    }

                    override fun onError(e: QBResponseException) {
                        binding.progressBar.visibility = View.GONE
                        Log.d(TAG, "Leaving Dialog Error: " + e.message)
                        longToast(R.string.error_leave_chat)
                    }
                })
            }
//            else if(place == "Complete"){
//                binding.progressBar.visibility = View.VISIBLE
//
//                teamDetailsVM.callChangeStatusAPI(taskId!!)
//
//            }
        }
    }

    override fun onDialogCreated(chatDialog: QBChatDialog) {}

    override fun onDialogUpdated(chatDialog: String) {}

    override fun onNewDialogLoaded(chatDialog: QBChatDialog) {}

}