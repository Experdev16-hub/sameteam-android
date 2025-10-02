package com.example.sameteam.splash

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Event

class SplashVM(val context: Application) : BaseViewModel(context) {

    private var messageString = MutableLiveData<Event<String>>()

    private fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun start() {

        object : CountDownTimer(2000, 500) {

            override fun onTick(millisUntilFinished: Long) {
            }

            override fun onFinish() {
                setMessage("Navigate")
            }
        }.start()
    }

    fun observedChanges() = messageString
}