package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.example.sameteam.BuildConfig
import com.example.sameteam.R
import com.example.sameteam.amazonS3.S3Util
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.ConstantsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.CreateTaskRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File


class CreateTaskVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "CreateTaskVM"

    private var messageString = MutableLiveData<Event<String>>()
    private var timesString = MutableLiveData<Event<String>>()
    private var specificDateString = MutableLiveData<Event<String>>()
    var createTaskRequestModel = CreateTaskRequestModel()
    val REQUEST_PERMISSION_SETTING = 101
    var selectedPath: String? = null
    var imageName: String? = null
    var eventList = MutableLiveData<Event<ArrayList<EventModel>>>()
    var participants = MutableLiveData<Event<ArrayList<UserModel>>>()
    var selectedUsers = MutableLiveData<Event<ArrayList<UserModel>>>()
    var taskId: Int? = null

    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString


    //set number of times
    fun onRepeatTimesClicked(value: String) {
        timesString.value = Event(value)
    }

    // Number of times change listener, CustomRepeatTaskBottomSheet is listening
    fun observeTimes(): LiveData<Event<String>> {
        return timesString
    }


    //set end date
    fun onSpecificDateClicked(value: String, location: String) {
        when (location) {
            Constants.SPECIFIC_DATE -> specificDateString.value = Event(value)
        }
    }

    // End Date change listener, CustomRepeatTaskBottomSheet is listening
    fun observeSpecificDate(): LiveData<Event<String>> {
        return specificDateString
    }


    //set events from API call
    private fun setEventList(list: ArrayList<EventModel>) {
        eventList.value = Event(list)
    }

    // Observe events list in CreateTaskActivity
    fun observeEventList() = eventList


    //set participants from GetEventsAndUser API call
    private fun setParticipants(list: ArrayList<UserModel>) {
        participants.value = Event(list)
    }

    // Observe participants list in CreateTaskActivity
    fun observeParticipants() = participants


    //set selected users from UserDirectoryAdapter
    fun onInviteUserClicked(list: ArrayList<UserModel>) {
        selectedUsers.value = Event(list)
    }

    // Observe selected users list in InvitePeopleBottom Sheet
    fun observeSelectedUsers() = selectedUsers


    fun onCreateClicked() {
        Log.d(TAG, "onClicked: ${Gson().toJson(createTaskRequestModel)}")
        if (isValid()) {
            Log.d(TAG, "onCreateClicked: Start")
            setMessage("CheckTime")
        }
    }

    //Field validations before API call
    fun isValid(): Boolean {
        setMessage(Constants.SHOW_PROGRESS)

        if (createTaskRequestModel.name.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_activity_name_error))
            return false
        }
//        else if (createTaskRequestModel.description.isNullOrBlank()) {
//            setMessage(context.getString(R.string.enter_description))
//            return false
//        } else if (createTaskRequestModel.event_id.isNullOrBlank()) {
//            setMessage(context.getString(R.string.select_event))
//            return false
//        }
        else if (createTaskRequestModel.start_date.isNullOrBlank()) {
            setMessage(context.getString(R.string.select_date))
            return false
        } else if (createTaskRequestModel.all_day == false && createTaskRequestModel.start_time.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_start))
            return false
        } else if (createTaskRequestModel.all_day == false && createTaskRequestModel.end_time.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_end))
            return false
        }
//        else if(createTaskRequestModel.all_day == false && (timeStringToDate(createTaskRequestModel.start_time.toString()) >= timeStringToDate(createTaskRequestModel.end_time.toString()) )){
//            setMessage(context.getString(R.string.enter_valid_time))
//            return false
//        }
//        else if(createTaskRequestModel.participant_ids.isNullOrEmpty()){
//            setMessage(context.getString(R.string.select_participants))
//            return false
//        } else if (createTaskRequestModel.location.isNullOrBlank()) {
//            setMessage(context.getString(R.string.enter_location))
//            return false
//        }
        else return true
    }

    fun onTaskImageClicked(view: View) {
        Dexter.withContext(context)
            .withPermissions(Utils.getPermissionAsPerAndroidVersion())
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        showImagePickerOptions(view)
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts(
                            "package",
                            ((view.context as Activity?)!!).packageName,
                            null
                        )
                        intent.data = uri
                        ((view.context as Activity?)!!).startActivityForResult(
                            intent,
                            REQUEST_PERMISSION_SETTING
                        )
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

    private fun showImagePickerOptions(view: View) {
        ImagePickerActivity.showImagePickerOptions(
            (view.context as Activity?)!!,
            object : ImagePickerActivity.PickerOptionListener {
                override fun onTakeVideoSelected() {

                }

                override fun onTakeCameraSelected() {
                    setMessage(Constants.CAMERA_INTENT)
                }

                override fun onChooseGallerySelected() {
                    setMessage(Constants.GALLERY_INTENT)
                }
            },
            2
        )
    }

//    fun checkTaskImage(mContext: Context, isEdit: Boolean) {
//        if(isEdit){
//            if (selectedPath.isNullOrBlank() || imageName.isNullOrBlank()) {
//                createTaskRequestModel.image_url = ""
//                callEditTaskAPI()
//            }
//            else
//                uploadTaskImage(selectedPath!!,imageName!!,mContext, isEdit)
//        }
//        else{
//            if (selectedPath.isNullOrBlank() || imageName.isNullOrBlank()) {
//                createTaskRequestModel.image_url = ""
//                callCreateTaskAPI()
//            }
//            else
//                uploadTaskImage(selectedPath!!,imageName!!,mContext, isEdit)
//        }
//
//
//    }

    fun uploadTaskImage(mSelectedMediaPath: String, imageFileName: String, context: Context) {

        val key = "sameteam/taskImages/$imageFileName"
        val transferUtility = S3Util.getTransferUtility(context)

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
                    createTaskRequestModel.image_url = baseUrl + key
                    setMessage("ImageUploaded")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {

            }

            override fun onError(id: Int, ex: Exception?) {
                setMessage(Constants.HIDE_PROGRESS)
                setMessage(context.getString(R.string.something_went_wrong))
            }

        })
    }

    fun callCreateTaskAPI() {
        setMessage(Constants.SHOW_PROGRESS)
        Log.d(TAG, "CreateActivityRequestModel: ${Gson().toJson(createTaskRequestModel)}")
        APITask.getInstance().callCreateTask(this, createTaskRequestModel)
            ?.let { mDisposable?.add(it) }
    }

    fun callEditTaskAPI() {
        setMessage(Constants.SHOW_PROGRESS)
        Log.d(TAG, "EditActivityRequestModel: ${Gson().toJson(createTaskRequestModel)}")
        APITask.getInstance().callEditTask(this, createTaskRequestModel)
            ?.let { mDisposable?.add(it) }
    }

    fun getEventsAndParticipants() {
        setMessage(Constants.VISIBLE)
        val commonModel = CommonModel()
        APITask.getInstance().callEventsAndParticipants(this, commonModel)
            ?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        if (requestCode == 1) {
            setMessage(Constants.HIDE_PROGRESS)
            Toast.makeText(
                context,
                (response as TaskDetailsResponseModel).message,
                Toast.LENGTH_LONG
            )
                .show()
            taskId = (response as TaskDetailsResponseModel).data.id
            setMessage(Constants.NAVIGATE)
        } else if (requestCode == 2) {
            val res = (response as ConstantsResponseModel).data
            Log.d(TAG, "onResponseReceived: ${res.events.size} ${res.participants.size}")
            if (!res.events.isNullOrEmpty()) {
                setEventList(res.events)
            }
            if (!res.participants.isNullOrEmpty()) {
                setParticipants(res.participants)
            }
            setMessage(Constants.HIDE)
        }

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        if (requestCode == 1)
            setMessage(Constants.HIDE_PROGRESS)
        else
            setMessage(Constants.HIDE)


        Log.d(TAG, "Response EventsParticipants API Error : Code $responseCode  Message $message")
        setMessage(message)
    }


}