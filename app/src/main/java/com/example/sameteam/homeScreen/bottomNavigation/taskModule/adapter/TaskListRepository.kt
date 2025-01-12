package com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.example.sameteam.helper.Constants
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.retrofit.APICall


class TaskListRepository(private val apiCall: APICall, private val date1: String, private val date2: String, private val onlyAccepted:Boolean) {

    fun letTaskListLiveData(pagingConfig: PagingConfig = getDefaultPageConfig()) : LiveData<PagingData<TaskDetailsResponseModel.Data>>{
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { TaskPagingSource(apiCall, date1, date2, onlyAccepted) }
        ).liveData
    }

    private fun getDefaultPageConfig(): PagingConfig {
        return PagingConfig(pageSize = Constants.DEFAULT_PAGE_SIZE, enablePlaceholders = false)
    }
}