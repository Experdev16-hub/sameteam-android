package com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter

import android.util.Log
import android.widget.Toast
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.sameteam.helper.localToUTCTimestamp
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskListRequestModel
import com.example.sameteam.retrofit.APICall
import com.example.sameteam.retrofit.APITask
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class TaskPagingSource(
    private val apiCall: APICall, private val date1: String, private val date2: String,
    private val onlyAccepted: Boolean
) : PagingSource<Int, TaskDetailsResponseModel.Data>() {

    override fun getRefreshKey(state: PagingState<Int, TaskDetailsResponseModel.Data>): Int {
        return 0
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TaskDetailsResponseModel.Data> {
        try {

            val requestModel = TaskListRequestModel()
            val startTime = "12:00 am"
            val endTime = "11:59 pm"
            val mTimeZone =
                SimpleDateFormat("Z", Locale.getDefault()).format(System.currentTimeMillis())

            Log.i("Date",date1)
            val startDateTimeString = date1 + "T" + startTime + ".000" + mTimeZone
            val endDateTimeString = date2 + "T" + endTime + ".000" + mTimeZone

            requestModel.date1_timestamp = localToUTCTimestamp(startDateTimeString).toString()
            requestModel.date2_timestamp = localToUTCTimestamp(endDateTimeString).toString()
            requestModel.only_accepted = onlyAccepted

            if (params.key != null) {
                requestModel.page = params.key!!
            } else {
                requestModel.page = 0
            }

            val response = apiCall.allTasks(requestModel)

            if (response.code() == 200) {
                val responseData = mutableListOf<TaskDetailsResponseModel.Data>()

                val tasks = response.body()?.data?.tasks

                if (!tasks.isNullOrEmpty()) {
                    for (item in tasks) {
                        responseData.add(item)
                    }
                }

                val prevPage: Int?
                val nextPage: Int?

                if (responseData.isNullOrEmpty()) {
                    if (params.key == null || params.key == 0) {
                        prevPage = null
                        nextPage = null
                    } else {
                        prevPage = params.key!! - 1
                        nextPage = null
                    }
                } else {
                    if (params.key == null || params.key == 0) {
                        prevPage = null
                        nextPage = 1
                    } else {
                        prevPage = params.key!! - 1
                        nextPage = params.key!! + 1
                    }
                }

                return LoadResult.Page(
                    data = responseData,
                    prevKey = prevPage,
                    nextKey = nextPage
                )
            } else if (response.code() == 401) {
                return LoadResult.Page(
                    data = emptyList(),
                    prevKey = null,
                    nextKey = null
                )
            } else {
                Log.e("06/12 response.code() -=-=-=>", response.code().toString())
                val e: Exception
                return if (response.code() == 400) {
                    e = Exception("Bad Request")
                    LoadResult.Error(e)
                } else if (response.code() == 404) {
                    e = Exception("Not Found")
                    LoadResult.Error(e)
                } else if (response.code() == 500) {
                    e = Exception("Internal Server Error")
                    LoadResult.Error(e)
                } else {
                    e = Exception("Something went wrong, Please try later")
                    LoadResult.Error(e)
                }
            }

        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }
}