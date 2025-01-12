package com.example.sameteam.homeScreen.bottomNavigation.taskModule.model

import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel

class TaskListResponseModel : BaseResponse<TaskListResponseModel.Data>() {
    inner class Data(
        var total_records : Int,
        var tasks: ArrayList<TaskDetailsResponseModel.Data> = ArrayList()
    )
}