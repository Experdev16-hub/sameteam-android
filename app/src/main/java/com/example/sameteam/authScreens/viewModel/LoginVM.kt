package com.example.sameteam.authScreens.viewModel

import android.app.Application
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.LoginRequestModel
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson

class LoginVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "Login"

    var loginReqModel = LoginRequestModel()

    private var messageString = MutableLiveData<Event<String>>()
    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun onLoginPressed(){
        if(isValid()){
            loginReqModel.email = loginReqModel.email?.trim()
            callLoginAPI()
        }
    }

    private fun isValid(): Boolean {


        if (loginReqModel.email.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_email))
            return false
        }
//        else if (!Patterns.EMAIL_ADDRESS.matcher(loginReqModel.email.toString()).matches()) {
//            setMessage(context.getString(R.string.enter_valid_email))
//            return false
//        }
        else if (loginReqModel.password.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_password))
            return false
        }
//        else if (!Utils.isValidPassword(loginReqModel.password!!)) {
//            setMessage(context.getString(R.string.enter_valid_password))
//            return false
//        }
        else return true
    }

    private fun callLoginAPI(){
        setMessage(Constants.VISIBLE)

        FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
            Log.d("FCM Login", "Token: $result")
            SharedPrefs.storeFcmToken(MyApplication.getInstance(), result)
            loginReqModel.device_token = result
            Log.d(TAG, "Request Login API ${Gson().toJson(loginReqModel)}")
            APITask.getInstance().callLogin(this,loginReqModel)?.let { mDisposable?.add(it) }
        }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        SharedPrefs.setUser(context, (response as LoginResponseModel).data)
        SharedPrefs.storeToken(context, (response as LoginResponseModel).data.token!!)
        setMessage(Constants.HIDE)
        val user = SharedPrefs.getUser(context)
        if(user?.is_approved == true)
            setMessage(Constants.NAVIGATE)
//        else
//            setMessage("Approval")

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Login API Error : Code $responseCode  Message $message")
        when (responseCode) {
            203 -> setMessage("Approval")
            else -> setMessage(message)
        }
    }


}