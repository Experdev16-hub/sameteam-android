package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

import com.example.sameteam.base.BaseResponse

class TaskMonthResponseModel : BaseResponse<TaskMonthResponseModel.Data>() {

    inner class Data(
        var repeat_date: ArrayList<String> = ArrayList(),
        var total_records: Int
    )
}