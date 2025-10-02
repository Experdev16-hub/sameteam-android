package com.example.sameteam.homeScreen.drawerNavigation.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.MyApplication
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.CommonModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener

class ProfileVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "ProfileVM"

    private var messageString = MutableLiveData<Event<String>>()

    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun onLogout() {
        setMessage(Constants.VISIBLE)
        val hashmap = HashMap<String, String>()
        val token = SharedPrefs.getFcmToken(MyApplication.getInstance())
        if (token != null) {
            hashmap["device_token"] = token
        }
        APITask.getInstance().callLogout(this, hashmap)?.let { mDisposable?.add(it) }
    }

    fun callNotificationAPI(isCheck: Boolean) {
        setMessage(Constants.VISIBLE)
        val hashmap = HashMap<String, String>()
        if (isCheck) hashmap["notification_status"] = "on"
        else hashmap["notification_status"] = "off"

        APITask.getInstance().callNotificationStatus(this, hashmap)?.let { mDisposable?.add(it) }
    }

    fun callDeleteUserAccount() {
        setMessage(Constants.VISIBLE)
        val commonModel = CommonModel()

        APITask.getInstance().callDeleteUserAccount(this, commonModel)?.let { mDisposable?.add(it) }
    }

    fun callMyProfile() {
        APITask.getInstance().callFetchUser(this)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        when (requestCode) {
            1 -> {
                setMessage(Constants.NAVIGATE)
            }
            2 -> {
                setMessage(Constants.HIDE)

                Toast.makeText(context, (response as LoginResponseModel).message, Toast.LENGTH_SHORT)
                    .show()

                SharedPrefs.setUser(context, (response as LoginResponseModel).data)

                Log.d(TAG, "onResponseReceived: ${(response as LoginResponseModel).data}")

            }
            4 -> {
                setMessage(Constants.DELETE_ACCOUNT)
            }
            9 -> {
                val res = response as RegisterResponseModel

                val user = SharedPrefs.getUser(context)

                user?.plan_upgrade = res.data?.plan_upgrade

                if (user != null) {
                    SharedPrefs.setUser(context, user)
                }

                setMessage(Constants.PLAN_STATUS)

            }
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        if (responseCode == 0) {
            setMessage(Constants.FORCE_LOGOUT)
        }
        if (requestCode == 9) {
            setMessage(Constants.PLAN_STATUS)
        }
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response Logout API Error : Code $responseCode  Message $message")
        setMessage(message)
    }

}