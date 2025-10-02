package com.example.sameteam.homeScreen

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener

class HomeVM(val context: Application): BaseViewModel(context), OnResponseListener {

    private var messageString = MutableLiveData<Event<String>>()
    private var rightIconString = MutableLiveData<Event<String>>()


    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    // Set Confirm Dialog for TaskDetailsBottomSheet
    fun callConfirm(place: String){
        setMessage(place)
    }
    // Observe OnConfirm click in TaskDetailsBottomSheet
    fun onConfirm(): LiveData<Event<String>> {
        return messageString
    }
    fun callMyProfile() {
        APITask.getInstance().callFetchUser(this)?.let { mDisposable?.add(it) }
    }

    // Set RightIcon click for ChatFragment
    fun setRightIcon(msg: String) {
        rightIconString.value = Event(msg)
    }
    // Observe onRightIconClicked in Chat Fragment
    fun observeRightIcon(): LiveData<Event<String>> {
        return rightIconString
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        if (requestCode==9){
            val res = response as RegisterResponseModel

            val user = SharedPrefs.getUser(context)

            user?.plan_upgrade= res.data?.plan_upgrade

            if (user != null) {
                SharedPrefs.setUser(context,user)
            }

            setMessage(Constants.PLAN_STATUS)

        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.PLAN_STATUS)

    }

}