package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.liveData
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.helper.Constants
import com.example.sameteam.retrofit.APICall

class AllUserListRepository(private val apiCall: APICall) {

    fun letUsersListLiveData(pagingConfig: PagingConfig = getDefaultPageConfig()) : LiveData<PagingData<UserModel>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = { AllUsersPagingSource(apiCall) }
        ).liveData
    }

    private fun getDefaultPageConfig(): PagingConfig {
        return PagingConfig(pageSize = Constants.DEFAULT_PAGE_SIZE, enablePlaceholders = false)
    }
}