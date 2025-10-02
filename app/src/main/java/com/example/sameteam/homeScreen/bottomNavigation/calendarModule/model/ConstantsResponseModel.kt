package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.authScreens.model.RegisterResponseModel
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse

class ConstantsResponseModel : BaseResponse<ConstantsResponseModel.Data>() {

    inner class Data(
        var events : ArrayList<EventModel> = ArrayList(),
        var participants : ArrayList<UserModel> = ArrayList(),
    )
}