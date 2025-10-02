package com.example.sameteam.homeScreen.drawerNavigation.model

import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.AllUsersResponseModel

class GetUserListResponseModel : BaseResponse<GetUserListResponseModel.Data>() {

    inner class Data(
        var total_records : Int,
        var user_list: ArrayList<UserModel> = ArrayList()
    )
}