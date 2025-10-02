package com.example.sameteam.homeScreen.bottomNavigation.taskModule.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListRepository
import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.retrofit.Retrofit

class AllTasksFragVM(val context: Application): BaseViewModel(context), OnResponseListener {
    private val TAG = "AllTasksFragVM"

    private var messageString = MutableLiveData<Event<String>>()
    private val apiCall: APICall = Retrofit.getRetrofit().create(APICall::class.java)

    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun callMyProfile(){
        APITask.getInstance().callFetchUser(this)?.let {  mDisposable?.add(it) }
    }

    fun fetchTasksLiveData(date1: String, date2: String): LiveData<PagingData<TaskDetailsResponseModel.Data>>{
        val repository = TaskListRepository(apiCall, date1, date2, false)
        return repository.letTaskListLiveData().map{ list ->
            list.map { it }
        }.cachedIn(viewModelScope)
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {

    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        if(responseCode == 0) {
            setMessage(Constants.FORCE_LOGOUT)
        }
    }


}