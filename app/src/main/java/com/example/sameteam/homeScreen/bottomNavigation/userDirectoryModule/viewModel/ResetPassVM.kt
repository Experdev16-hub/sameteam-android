package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.ChangePasswordRequestModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import java.util.regex.Pattern

class ResetPassVM(val context:Application) : BaseViewModel(context),OnResponseListener {
    private val TAG = "ResetPassVM"

    var changePasswordRequestModel = ChangePasswordRequestModel()

    private var messageString = MutableLiveData<Event<String>>()
    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun onSendClicked(){
        if(isValid()){
            setMessage(Constants.VISIBLE)
            APITask.getInstance().callChangePassword(this,changePasswordRequestModel)?.let { mDisposable?.add(it) }
        }
    }

    fun isValid(): Boolean{
        if (changePasswordRequestModel.old_password.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_old))
            return false
        } else if (changePasswordRequestModel.password.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_new_password))
            return false
        } else if (!isValidPassword(changePasswordRequestModel.password.toString())) {
            setMessage(context.getString(R.string.invalid_password_error))
            return false
        } else if (changePasswordRequestModel.confirmPassword.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_confirm_password))
            return false
        } else if (!changePasswordRequestModel.confirmPassword.equals(changePasswordRequestModel.password)) {
            setMessage(context.getString(R.string.no_password_match))
            return false
        } else if(changePasswordRequestModel.old_password.equals(changePasswordRequestModel.password)){
            setMessage(context.getString(R.string.same_password_old))
            return false
        }
        else return true
    }

    private fun isValidPassword(password: String): Boolean {
        return Pattern.compile("(?=.*[0-9])(?=.*[a-z]*[A-Z])(?=.*[!@#$%^&+*=])(?=\\S+$).{8,}")
            .matcher(password)
            .matches()
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        setMessage(Constants.HIDE)
        Toast.makeText(MyApplication.getInstance(), (response as RegisterResponseModel).message, Toast.LENGTH_LONG).show()
        setMessage(Constants.NAVIGATE)
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Change Password API Error : Code $responseCode  Message $message")
        setMessage(message)
    }
}