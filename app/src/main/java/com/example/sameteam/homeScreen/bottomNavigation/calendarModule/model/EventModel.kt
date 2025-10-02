package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

import com.example.sameteam.authScreens.model.UserModel


class EventModel {
    var id: Int? = 0
    var title: String? = null
    var status: String? = null
    var event_description: String? = null
    var start_date: String? = null
    var start_time: String? = null
    var end_date: String? = null
    var end_time: String? = null
    var colour: String? = null
    var event_user: ArrayList<EventUser> = ArrayList()
    var editable: Boolean? = false

    class EventUser {
        var id: Int? = 0
        var user: UserModel? = null
    }
}