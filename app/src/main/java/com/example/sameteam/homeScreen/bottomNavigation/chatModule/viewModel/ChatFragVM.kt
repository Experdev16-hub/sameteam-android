package com.example.sameteam.homeScreen.bottomNavigation.chatModule.viewModel

import android.app.Application
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskStatusRequestModel
import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.retrofit.Retrofit

class ChatFragVM(val context: Application) : BaseViewModel(context), OnResponseListener {

    private var messageString = MutableLiveData<Event<String>>()


    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun callMyProfile() {
        APITask.getInstance().callFetchUser(this)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        if(responseCode == 0) {
            setMessage(Constants.FORCE_LOGOUT)
        }
    }
}