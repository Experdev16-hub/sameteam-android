package com.example.sameteam.homeScreen.drawerNavigation.model

import com.example.sameteam.base.BaseResponse
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel

class MyEventListResponseModel: BaseResponse<MyEventListResponseModel.Data>() {

    inner class Data(
        var total_records : Int,
        var events: ArrayList<EventModel> = ArrayList()
    )
}