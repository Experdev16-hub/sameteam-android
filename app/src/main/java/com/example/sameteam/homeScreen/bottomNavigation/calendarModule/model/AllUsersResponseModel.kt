package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse

class AllUsersResponseModel: BaseResponse<AllUsersResponseModel.Data>() {

    inner class Data(
        var total_records : Int,
        var users: ArrayList<UserModel> = ArrayList()
    )
}