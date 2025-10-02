package com.example.sameteam.retrofit

import com.example.sameteam.authScreens.model.LoginRequestModel
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.authScreens.model.RegisterRequestModel
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.helper.CommonModel
import com.example.sameteam.helper.RefreshTokenResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.*
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model.NotificationsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.ChangePasswordRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskListRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskListResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.AddUserToContactRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.model.EditQBRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.*
import com.example.sameteam.homeScreen.drawerNavigation.viewModel.AddEventVM
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.*

interface APICall {

    @POST(API.LOGIN)
    fun login(@Body params: LoginRequestModel): Observable<Response<LoginResponseModel>>

    @POST(API.REGISTER)
    fun register(@Body params: RegisterRequestModel): Observable<Response<RegisterResponseModel>>

    @GET("${API.REFRESH_TOKEN}/{refreshToken}")
    fun getAccessToken(@Path("refreshToken")refreshToken: String): Observable<Response<RefreshTokenResponseModel>>

    @PUT(API.USER)
    fun editUser(@Body params: LoginResponseModel.User): Observable<Response<RegisterResponseModel>>

    @GET(API.USER)
    fun fetchUser(): Observable<Response<RegisterResponseModel>>

    @GET("${API.FORGOT_PASSWORD}/{email}")
    fun forgotPassword(@Path("email")email: String): Observable<Response<CommonModel>>

    @PUT(API.CHANGE_PASSWORD)
    fun changePassword(@Body params: ChangePasswordRequestModel): Observable<Response<RegisterResponseModel>>

    @POST(API.LOGOUT)
    fun logout(@Body param: HashMap<String,String>) : Observable<Response<CommonModel>>

    @POST(API.CREATE_TASK)
    fun createTask(@Body params: CreateTaskRequestModel): Observable<Response<TaskDetailsResponseModel>>

    @PUT(API.CREATE_TASK)
    fun editTask(@Body params: CreateTaskRequestModel): Observable<Response<TaskDetailsResponseModel>>

    @DELETE("${API.CREATE_TASK}/{taskId}")
    fun deleteTask(@Path("taskId") taskId: Int): Observable<Response<CommonModel>>

    @GET(API.EVENTS_AND_PARTICIPANTS)
    fun getEventsAndParticipants(): Observable<Response<ConstantsResponseModel>>

    @GET("${API.CREATE_TASK}/{taskId}")
    fun getTaskById(@Path("taskId") taskId: Int): Observable<Response<TaskDetailsResponseModel>>

    @PUT(API.CHANGE_TASK_STATUS)
    fun changeTaskStatus(@Body params: TaskStatusRequestModel) : Observable<Response<TaskDetailsResponseModel>>

    @POST(API.MY_EVENTS)
    fun myEvents(): Observable<Response<MyEventListResponseModel>>

//    @POST(API.ALL_TASKS)
//    fun allTasks(@Body param: HashMap<String,Int>): Observable<Response<TaskListResponseModel>>

    @POST(API.ALL_TASKS)
    suspend fun allTasks(@Body param: TaskListRequestModel): Response<TaskListResponseModel>

    @PUT(API.PARTICIPANT_RESPONSE)
    fun participantResponse(@Body param: HashMap<String,String>) : Observable<Response<TaskParticipantResponseModel>>

    @POST(API.USER_DIRECTORY)
    suspend fun getAllUsers(@Body param: HashMap<String,Int>): Response<AllUsersResponseModel>

    @POST(API.GET_MONTH_DATES)
    fun getMonthTaskDates(@Body params: HashMap<String,String>) : Observable<Response<TaskMonthResponseModel>>

    @PUT(API.CHANGE_NOTIFICATION_STATUS)
    fun changeNotiStatus(@Body param: HashMap<String,String>) : Observable<Response<LoginResponseModel>>

    @POST(API.GET_NOTIFICATIONS)
    fun getNotifications(@Body param: HashMap<String,Int>): Observable<Response<NotificationsResponseModel>>

    @POST(API.DELETE_ACCOUNT)
    fun deleteUserAccount(@Body param: CommonModel): Observable<Response<CommonModel>>

    @POST(API.GET_USER_LIST)
    fun getUserList(@Body param: GetUserListRequestModel): Observable<Response<GetUserListResponseModel>>

    @POST(API.ADD_EVENT)
    fun addEvent(@Body param: AddEventRequestModel): Observable<Response<CommonModel>>

    @POST(API.GET_EVENT_BY_ID)
    fun getEventById(@Body param: GetEventRequestModel): Observable<Response<GetEventResponseModel>>

    @POST(API.EDIT_EVENT)
    fun editEvent(@Body param: AddEventRequestModel): Observable<Response<CommonModel>>

    @POST(API.SIGN_UP_FOR_TASK)
    fun signUpForTask(@Body param: GetEventRequestModel): Observable<Response<CommonModel>>

    @POST(API.GET_PARTICIPANT_ID_LIST)
    fun getParticipantIdList(@Body param: GetUserListRequestModel): Observable<Response<GetParticipantIdListResponseModel>>

    @POST(API.ADD_USER_CONTACT)
    fun addUserContact(@Body param: AddUserToContactRequestModel): Observable<Response<CommonModel>>
    @POST(API.EDIT_QB_TEAM)
    fun editQBTeam(@Body param: EditQBRequestModel): Observable<Response<CommonModel>>

}