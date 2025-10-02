package com.example.sameteam.authScreens.viewModel

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.R
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener

class ForgotVM(val context: Application) : BaseViewModel(context),OnResponseListener{
    private val TAG = "ForgotVM"

    private var messageString = MutableLiveData<Event<String>>()
    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun onSendClicked(email: String?){
        if(email.isNullOrBlank()){
            setMessage(context.getString(R.string.enter_email))
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setMessage(context.getString(R.string.enter_valid_email))
        }
        else{
            setMessage(Constants.VISIBLE)
            APITask.getInstance().callForgotPassword(this,email)?.let { mDisposable?.add(it) }
        }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        Log.d(TAG, "onResponseReceived: $response")
        setMessage(Constants.HIDE)
        setMessage(Constants.NAVIGATE)
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Forgot Password API Error : Code $responseCode  Message $message")
        setMessage(message)
    }
}