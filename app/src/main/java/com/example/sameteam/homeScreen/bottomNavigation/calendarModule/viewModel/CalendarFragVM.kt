package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Event
import com.example.sameteam.helper.localToUTCTimestamp
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskMonthResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskStatusRequestModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.TaskListRepository
import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.APITask
import com.example.sameteam.retrofit.OnResponseListener
import com.example.sameteam.retrofit.Retrofit
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CalendarFragVM(val context: Application) : BaseViewModel(context), OnResponseListener {
    private val TAG = "CalendarFragVM"

    private var messageString = MutableLiveData<Event<String>>()
    private val apiCall: APICall = Retrofit.getRetrofit().create(APICall::class.java)
    var dateList = MutableLiveData<Event<ArrayList<String>>>()


    fun setMessage(msg: String) {
        messageString.value = Event(msg)
    }

    fun observedChanges() = messageString

    //set dates from API call
    private fun setDateList(list: ArrayList<String>) {
        dateList.value = Event(list)
    }

    // Observe date list in Calendar Fragment
    fun observeDateList() = dateList

    fun fetchTasksLiveData(date: String): LiveData<PagingData<TaskDetailsResponseModel.Data>> {
        val repository = TaskListRepository(apiCall, date, date, true)
        return repository.letTaskListLiveData().map { list ->
            list.map { it }
        }.cachedIn(viewModelScope)
    }

    fun callTaskChangeStatus(taskId: Int) {
        setMessage(Constants.VISIBLE)
        val statusModel = TaskStatusRequestModel(taskId)
        APITask.getInstance().callChangeTaskStatus(this, statusModel)?.let { mDisposable?.add(it) }
    }

    fun callTaskDates(date1: String, date2: String) {
        setMessage(Constants.VISIBLE)
        val hashMap = HashMap<String, String>()

        val startTime = "12:00 am"
        val endTime = "11:59 pm"
        val mTimeZone =
            SimpleDateFormat("Z", Locale.getDefault()).format(System.currentTimeMillis())

        val startDateTimeString = date1 + "T" + startTime + ".000" + mTimeZone
        val endDateTimeString = date2 + "T" + endTime + ".000" + mTimeZone

        hashMap["date1_timestamp"] = localToUTCTimestamp(startDateTimeString).toString()
        hashMap["date2_timestamp"] = localToUTCTimestamp(endDateTimeString).toString()
        hashMap["isAccepted"]= true.toString()
        APITask.getInstance().callGetMonthTasks(this, hashMap)?.let { mDisposable?.add(it) }
    }

    override fun <T> onResponseReceived(response: T, requestCode: Int) {
        Log.e(
            "09/01 onResponseReceived -=-=-=-=>",
            "response $response" + "-=--=-=>" + "requestCode ${requestCode.toString()}"
        )

        if (requestCode == 1) {
            val taskMonthResponseModel = (response as TaskMonthResponseModel)
            if (taskMonthResponseModel != null && taskMonthResponseModel.data != null && taskMonthResponseModel.data.repeat_date!=null && taskMonthResponseModel.data.repeat_date.size>0) {
                val dateArray = (response as TaskMonthResponseModel).data.repeat_date
                if (dateArray.isNotEmpty()) {
                    setDateList(dateArray)
//               for (i in dateArray){
//                   val temp = utcTimestampToLocalDateTime(i)
//                   Log.d(TAG, "Repeat Date $i  :  ${temp?.toLocalDate()}  ${temp?.toLocalTime()}")
//               }
                }
            }

            setMessage(Constants.HIDE)
        } else if (requestCode == 3) {
            setMessage(Constants.HIDE)
            Toast.makeText(
                context,
                (response as TaskDetailsResponseModel).message,
                Toast.LENGTH_LONG
            )
                .show()
            setMessage(Constants.NAVIGATE)
        }
    }

    override fun onResponseError(message: String, requestCode: Int, responseCode: Int) {
        Log.e(
            "09/01 onResponseError -=-=-=-=>",
            "Response Change Task API Error : Code $responseCode  Message $message"
        )
        setMessage(Constants.HIDE)

        if (responseCode == 0) {
            setMessage(Constants.FORCE_LOGOUT)
        } else if (requestCode == 3) {
            Log.d(TAG, "Response Change Task API Error : Code $responseCode  Message $message")
        } else
            Log.d(TAG, "Response Month Task dates API Error : Code $responseCode  Message $message")

        setMessage(message)
    }
}