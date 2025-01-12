package com.example.sameteam.homeScreen.drawerNavigation.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskListResponseModel
import com.example.sameteam.homeScreen.drawerNavigation.model.MyEventListResponseModel
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener

class MyEventsVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "MyEventVM"

    private var messageString = MutableLiveData<Event<String>>()
    var eventsList = MutableLiveData<Event<ArrayList<EventModel>>>()


    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString


    //set Events from API call
    private fun setEventsList(list: ArrayList<EventModel>){
        eventsList.value = Event(list)
    }
    // Observe Events list in MyEventsActivity
    fun observeEventsList() = eventsList

    fun callMyEvents(){
        setMessage(Constants.VISIBLE)
        APITask.getInstance().callMyEventsList(this)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        val arrayList = (response as MyEventListResponseModel).data.events
//        if(!arrayList.isNullOrEmpty())
        setEventsList(arrayList)
        setMessage(Constants.HIDE)

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)
        Log.d(TAG, "Response MyEventsList API Error : Code $responseCode  Message $message")
        setMessage(message)
    }
}