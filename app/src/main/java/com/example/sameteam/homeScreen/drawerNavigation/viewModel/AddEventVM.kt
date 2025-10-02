package com.example.sameteam.homeScreen.drawerNavigation.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.GetUserListRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.AddEventRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetEventRequestModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetEventResponseModel
import com.example.sameteam.homeScreen.drawerNavigation.model.GetUserListResponseModel
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import java.text.SimpleDateFormat
import java.util.*


class AddEventVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "AddEventVM"

    private var messageString = MutableLiveData<Event<String>>()

    private var selectedUsers = MutableLiveData<Event<ArrayList<UserModel>>>()

    private var users = MutableLiveData<Event<ArrayList<UserModel>>>()

    private var eventModel = MutableLiveData<Event<EventModel>>()

    var addEventRequestModel = AddEventRequestModel()

    var eventStartTimestamp: Long? = null
    var eventEndTimestamp: Long? = null


    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    private fun setUserList(list: ArrayList<UserModel>) {
        users.value = Event(list)
    }

    fun observeUsers() = users

    private fun setEventDetails(eventDetails: EventModel) {
        eventModel.value = Event(eventDetails)
    }

    fun observeEventDetails() = eventModel

    //set selected users from UserDirectoryAdapter
    fun onInviteUserClicked(list: ArrayList<UserModel>) {
        selectedUsers.value = Event(list)
    }

    // Observe selected users list in InvitePeopleBottom Sheet
    fun observeSelectedUsers() = selectedUsers

    fun getUserList(searchKey: String = "") {

        setMessage(Constants.VISIBLE)

        val requestModel = GetUserListRequestModel()
        requestModel.search = searchKey
        APITask.getInstance().getUserList(this, requestModel)?.let { mDisposable?.add(it) }

    }

    fun onCreateClicked(isEdit: Boolean) {
        if (isValid(isEdit)) {
            setMessage(Constants.VISIBLE)
            if (isEdit){
                APITask.getInstance().editEvent(this, addEventRequestModel)?.let { mDisposable?.add(it) }
            }
            else {
                APITask.getInstance().addEvent(this, addEventRequestModel)
                    ?.let { mDisposable?.add(it) }
            }
        }
    }

    fun getEventById(eventId: Int) {
        setMessage(Constants.VISIBLE)

        val getEventRequestModel = GetEventRequestModel()
        getEventRequestModel.id = eventId

        APITask.getInstance().getEventById(this, getEventRequestModel)?.let { mDisposable?.add(it) }

    }

    //Field validations before API call
    private fun isValid(isEdit: Boolean): Boolean {

        val c = Calendar.getInstance().time

        val df = SimpleDateFormat("dd-MMM-yyyy'T'hh:mm a.SSSZ", Locale.getDefault())
        val formattedDate: String = df.format(c)

        df.timeZone = TimeZone.getTimeZone("UTC")
        val date = df.parse(formattedDate)
        val currentUTCTimestamp = date.time / 1000

        setMessage(Constants.VISIBLE)
        return if (addEventRequestModel.title.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_event_title))
            false
        } else if (addEventRequestModel.start_date.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_event_start_date))
            false
        } else if (addEventRequestModel.start_time.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_event_start_time))
            false
        } else if (addEventRequestModel.end_date.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_event_end_date))
            false
        } else if (addEventRequestModel.end_time.isNullOrBlank()) {
            setMessage(context.getString(R.string.enter_event_end_time))
            false
        } else if (eventStartTimestamp == null || eventEndTimestamp == null) {
            setMessage(context.getString(R.string.select_start_end_time))
            false
        } else if (currentUTCTimestamp > eventStartTimestamp!! && !isEdit) {
            setMessage(context.getString(R.string.select_valid_start_time))
            false
        } else if (eventStartTimestamp!! > eventEndTimestamp!!) {
            setMessage(context.getString(R.string.select_valid_time))
            false
        } else if (addEventRequestModel.colour.isNullOrBlank()) {
            setMessage(context.getString(R.string.choose_event_color))
            false
        } else true
    }


    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        when (requestCode) {
            1 -> { // getUserList
                val res = (response as GetUserListResponseModel).data

                if (res.user_list.isNotEmpty()) {
                    setUserList(res.user_list)
                }

                setMessage(Constants.HIDE)
            }
            2 -> { // addEvent
                setMessage(Constants.NAVIGATE)
            }
            3 -> { // getEventById
                val eventDetails = (response as GetEventResponseModel).data

                setEventDetails(eventDetails)
            }
            4 -> { // editEvent
                setMessage(Constants.NAVIGATE)
            }
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        setMessage(Constants.HIDE)

        setMessage(message)
    }
}