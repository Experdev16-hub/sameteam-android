package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter.AllUserListRepository

import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.retrofit.Retrofit

class AllUsersVM(val context: Application): BaseViewModel(context){

    private val TAG = "AllUsersVM"

    private var messageString = MutableLiveData<Event<String>>()
    var userList = MutableLiveData<ArrayList<UserModel>>()
    var users = ArrayList<UserModel>()


    fun setUser(userModel: ArrayList<UserModel>){
        userList.value = userModel
    }

    fun getUsers(): LiveData<ArrayList<UserModel>> {
        return userList
    }

    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    private val apiCall: APICall = Retrofit.getRetrofit().create(APICall::class.java)

    fun fetchUsersLiveData(): LiveData<PagingData<UserModel>> {
        val repository = AllUserListRepository(apiCall)
        return repository.letUsersListLiveData().map{ list ->
            list.map { test ->
                users.add(test)
                setUser(users)
                test
            }
        }.cachedIn(viewModelScope)
    }

}