package com.example.sameteam.homeScreen.drawerNavigation.model

import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse

class GetParticipantIdListResponseModel  : BaseResponse<GetParticipantIdListResponseModel.Data>() {

    inner class Data(
        var userId_list: ArrayList<Int> = ArrayList()
    )
}