package com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.CommonModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.GetUserListRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.AddUserToContactRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.EditQBRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetParticipantIdListResponseModel
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.quickblox.users.model.QBUser

class AllUsersFragVM(val context: Application) : BaseViewModel(context), OnResponseListener {

    private var messageString = MutableLiveData<Event<String>>()
    var getParticipantIdListResponseModel = GetParticipantIdListResponseModel()

    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    fun getParticipantIdList() {
        setMessage(Constants.VISIBLE)
        val requestModel = GetUserListRequestModel()
        APITask.getInstance().getParticipantIdList(this, requestModel)?.let { mDisposable?.add(it) }
    }

    fun addUserToContacts(userIdList: List<Int>) {
        setMessage(Constants.VISIBLE)

        val requestModel = AddUserToContactRequestModel()
        requestModel.userId = userIdList
        APITask.getInstance().addUserContact(this, requestModel)?.let { mDisposable?.add(it) }
    }

    fun editQBTeam(
        groupUsersList: List<Int>,
        previousSelectedUsers: List<Int?>,
        dialogId: String
    ) {
        setMessage(Constants.VISIBLE)

        val requestModel = EditQBRequestModel()
        requestModel.oldUserId = previousSelectedUsers as List<Int>
        requestModel.newUserId = groupUsersList
        requestModel.teamId = dialogId
        APITask.getInstance().editQBTeam(this, requestModel)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        when (requestCode) {
            1 -> {
                getParticipantIdListResponseModel = response as GetParticipantIdListResponseModel
                setMessage(Constants.HIDE)
            }

            2 -> {
                val responseModel = response as CommonModel
                setMessage(responseModel.message)
                setMessage(Constants.NAVIGATE)
            }

            10 -> {
                val responseModel = response as CommonModel
                setMessage(responseModel.message)
            }
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)

        if (responseCode == 0) {
            setMessage(Constants.FORCE_LOGOUT)
        }
    }
}