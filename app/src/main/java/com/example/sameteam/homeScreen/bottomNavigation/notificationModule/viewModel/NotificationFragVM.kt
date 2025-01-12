package com.example.sameteam.homeScreen.bottomNavigation.notificationModule.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model.NotificationModel
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model.NotificationsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantResponseModel
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener

class NotificationFragVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "NotificationFragVM"

    private var messageString = MutableLiveData<Event<String>>()
    private var listData = MutableLiveData<Event<ArrayList<NotificationModel>>>()
    var totalRecords = 0
    var pageCounter = 0
    var taskParticipantResponseModel = TaskParticipantResponseModel()
    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun listDataObserver() = listData

    private fun setNotificationList(list: ArrayList<NotificationModel>) {
        listData.value = Event(list)
    }

    fun callGetNotifications(isPagination: Boolean = false, size: Int = 0) {
        val hashmap = HashMap<String, Int>()
        hashmap.clear()

        if (isPagination) {
            if (totalRecords > size) {
                hashmap["page"] = ++pageCounter
            } else {
                return
            }
        } else {
            pageCounter = 0
            hashmap["page"] = pageCounter
        }
        hashmap["size"] = 10
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callGetNotifications(this, hashmap).let { mDisposable?.add(it!!) }
    }


    fun callParticipantAPI(participantId: Int, response: String) {
        val hashmap = HashMap<String, String>()
        hashmap["id"] = participantId.toString()
        hashmap["response"] = response
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callParticipantResponse(this, hashmap).let { mDisposable?.add(it!!) }

    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        setMessage(Constants.HIDE)

        if (requestCode == 1) {
            val result = (response as NotificationsResponseModel).data
            totalRecords = result.total_records
            setNotificationList(result.notification)
        } else if (requestCode == 4) {
            taskParticipantResponseModel= (response as TaskParticipantResponseModel)
            setMessage(taskParticipantResponseModel.message)
            setMessage (Constants.NAVIGATE)
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)

        if (requestCode == 1) {
            Log.d(TAG, "Response Notification API Error : Code $responseCode  Message $message")
            setMessage(message)
        } else if (requestCode == 4) {
            Log.d(
                TAG,
                "Task participants response API Error : Code $responseCode  Message $message"
            )
            setMessage(message)
        }

    }

}