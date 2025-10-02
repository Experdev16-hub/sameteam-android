package com.example.sameteam.homeScreen.bottomNavigation.chatModule.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.CommonModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskStatusRequestModel
import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.retrofit.Retrofit

class TeamDetailsVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "TeamDetailsVM"

    private var messageString = MutableLiveData<Event<String>>()
    var taskDetails: TaskDetailsResponseModel.Data? = null


    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun getTaskById(taskId: Int){
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callGetTaskById(this, taskId)
    }

    fun callDeleteTaskAPI(taskId: Int) {
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callDeleteTask(this, taskId)
    }

    fun callChangeStatusAPI(taskId: Int) {
        setMessage(Constants.VISIBLE)
        val statusModel = TaskStatusRequestModel(taskId)
        APITask.getInstance().callChangeTaskStatus(this, statusModel)
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        setMessage(Constants.HIDE)
        if(requestCode == 1){  // Response of getTaskByID API
            taskDetails = (response as TaskDetailsResponseModel).data
            setMessage("setTaskDetails")
        }
        else if (requestCode == 2) {  // Response of Delete Task API
            setMessage((response as CommonModel).message)
            setMessage(Constants.NAVIGATE)
        }
        else if (requestCode == 3) {  // Response of ChangeTaskStatus API
            taskDetails = (response as TaskDetailsResponseModel).data
            setMessage((response as TaskDetailsResponseModel).message)
            setMessage("setTaskDetails")
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        if(requestCode == 1){
            Log.d(TAG, "Response TaskDetails API Error : Code $responseCode  Message $message")
            setMessage(message)
        }
        else if(requestCode == 2){
            Log.d(TAG, "Response Delete Task API Error : Code $responseCode  Message $message")
            setMessage(message)
        }
        else if(requestCode == 3){
            Log.d(TAG, "Response Change Task API Error : Code $responseCode  Message $message")
            setMessage(message)
        }
    }
}