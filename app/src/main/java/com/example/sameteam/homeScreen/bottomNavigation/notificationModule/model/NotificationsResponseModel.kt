package com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model

import com.example.sameteam.base.BaseResponse

class NotificationsResponseModel : BaseResponse<NotificationsResponseModel.Data>() {

    inner class Data(
        var total_records: Int,
        var notification: ArrayList<NotificationModel> = ArrayList()
    )
}