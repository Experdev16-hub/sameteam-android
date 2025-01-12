package com.example.sameteam.retrofit

import com.example.sameteam.authScreens.model.LoginRequestModel
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.authScreens.model.RegisterRequestModel
import com.example.sameteam.helper.CommonModel
import com.example.sameteam.helper.RefreshTokenRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.CreateTaskRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.GetUserListRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskStatusRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel.CalendarFragVM
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.viewModel.TaskFragVM
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.AddUserToContactRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.ChangePasswordRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.EditQBRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.AddEventRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetEventRequestModel
import io.reactivex.disposables.Disposable

class APITask : BaseAPITask() {

    private val apiCall: APICall = Retrofit.getRetrofit()
        .create(APICall::class.java)

    companion object Singleton {
        fun getInstance(): APITask {
            return APITask()
        }
    }

    fun callLogin(listener: OnResponseListener, params: LoginRequestModel): Disposable? {
        return getRequest(apiCall.login(params), listener, 1)
    }

    fun callRegister(listener: OnResponseListener, params: RegisterRequestModel): Disposable? {
        return getRequest(apiCall.register(params), listener, 1)
    }

    fun callAccessToken(
        listener: OnResponseListener,
        params: RefreshTokenRequestModel,
        requestCode: Int
    ): Disposable? {
        return getRequest(apiCall.getAccessToken(params.refresh_token!!), listener, requestCode)
    }

    fun callEditUser(listener: OnResponseListener, params: LoginResponseModel.User): Disposable? {
        return getRequest(apiCall.editUser(params), listener, 1)
    }

    fun callFetchUser(listener: OnResponseListener): Disposable? {
        return getRequest(apiCall.fetchUser(), listener, 9)
    }

    fun callForgotPassword(listener: OnResponseListener, params: String): Disposable? {
        return getRequest(apiCall.forgotPassword(params), listener, 1)
    }

    fun callChangePassword(
        listener: OnResponseListener,
        params: ChangePasswordRequestModel
    ): Disposable? {
        return getRequest(apiCall.changePassword(params), listener, 1)
    }

    fun callLogout(listener: OnResponseListener, params: HashMap<String, String>): Disposable? {
        return getRequest(apiCall.logout(params), listener, 1)
    }

    fun callCreateTask(listener: OnResponseListener, params: CreateTaskRequestModel): Disposable? {
        return getRequest(apiCall.createTask(params), listener, 1)
    }

    fun callEditTask(listener: OnResponseListener, params: CreateTaskRequestModel): Disposable? {
        return getRequest(apiCall.editTask(params), listener, 1)
    }

    fun callDeleteTask(listener: OnResponseListener, params: Int): Disposable? {
        return getRequest(apiCall.deleteTask(params), listener, 2)
    }

    fun callEventsAndParticipants(listener: OnResponseListener, params: CommonModel): Disposable? {
        return getRequest(apiCall.getEventsAndParticipants(), listener, 2)
    }

    fun callGetTaskById(listener: OnResponseListener, params: Int): Disposable? {
        return getRequest(apiCall.getTaskById(params), listener, 1)
    }

    fun callChangeTaskStatus(
        listener: OnResponseListener,
        params: TaskStatusRequestModel
    ): Disposable? {
        return getRequest(apiCall.changeTaskStatus(params), listener, 3)
    }

    fun callMyEventsList(listener: OnResponseListener): Disposable? {
        return getRequest(apiCall.myEvents(), listener, 1)
    }

//    fun callAllTasksList(listener: OnResponseListener, params: HashMap<String,Int>) : Disposable? {
//        return getRequest(apiCall.allTasks(params), listener, 1)
//    }

    fun callParticipantResponse(
        listener: OnResponseListener,
        params: HashMap<String, String>
    ): Disposable? {
        return getRequest(apiCall.participantResponse(params), listener, 4)
    }

    fun callGetMonthTasks(
        listener: CalendarFragVM,
        params: HashMap<String, String>
    ): Disposable? {
        return getRequest(apiCall.getMonthTaskDates(params), listener, 1)
    }
    fun callTaskGetMonthTasks(
        listener: TaskFragVM,
        params: HashMap<String, String>
    ): Disposable? {
        return getRequest(apiCall.getMonthTaskDates(params), listener, 1)
    }
    fun callNotificationStatus(
        listener: OnResponseListener,
        params: HashMap<String, String>
    ): Disposable? {
        return getRequest(apiCall.changeNotiStatus(params), listener, 2)
    }

    fun callGetNotifications(
        listener: OnResponseListener,
        params: HashMap<String, Int>
    ): Disposable? {
        return getRequest(apiCall.getNotifications(params), listener, 1)
    }

    fun callDeleteUserAccount(listener: OnResponseListener, params: CommonModel): Disposable? {
        return getRequest(apiCall.deleteUserAccount(params), listener, 4)
    }

    fun getUserList(listener: OnResponseListener, params: GetUserListRequestModel): Disposable? {
        return getRequest(apiCall.getUserList(params), listener, 1)
    }

    fun addEvent(listener: OnResponseListener, params: AddEventRequestModel): Disposable? {
        return getRequest(apiCall.addEvent(params), listener, 2)
    }

    fun getEventById(listener: OnResponseListener, params: GetEventRequestModel): Disposable? {
        return getRequest(apiCall.getEventById(params), listener, 3)
    }

    fun editEvent(listener: OnResponseListener, params: AddEventRequestModel): Disposable? {
        return getRequest(apiCall.editEvent(params), listener, 4)
    }

    fun signUpForTask(listener: OnResponseListener, params: GetEventRequestModel): Disposable? {
        return getRequest(apiCall.signUpForTask(params), listener, 5)
    }

    fun getParticipantIdList(
        listener: OnResponseListener,
        params: GetUserListRequestModel
    ): Disposable? {
        return getRequest(apiCall.getParticipantIdList(params), listener, 1)
    }

    fun addUserContact(
        listener: OnResponseListener,
        params: AddUserToContactRequestModel
    ): Disposable? {
        return getRequest(apiCall.addUserContact(params), listener, 2)
    }
    fun editQBTeam(
        listener: OnResponseListener,
        params: EditQBRequestModel
    ): Disposable? {
        return getRequest(apiCall.editQBTeam(params), listener, 10)
    }
}