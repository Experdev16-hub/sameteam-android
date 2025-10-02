package com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model

class SmallTaskModel (
    val task_name: String,
    val start_date: String,
    val start_time: String,
    val end_time: String,
    val all_day: Boolean,
    var start_time_stamp: Long? = null,
    var end_time_stamp: Long? = null
)