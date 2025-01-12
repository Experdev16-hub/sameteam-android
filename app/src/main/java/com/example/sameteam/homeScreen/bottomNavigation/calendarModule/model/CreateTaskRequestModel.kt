package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

class CreateTaskRequestModel {

    var id: Int? = null
    var image_url: String? = ""
    var name: String? = null
    var description: String? = ""
    var location: String? = ""
    var remind_me: ArrayList<RemindMeModel> = ArrayList()
    var event_id: String? = ""
    var participant_ids: ArrayList<Int> = ArrayList()
    var start_date: String? = null
    var start_time: String? = ""
    var end_time: String? = ""
    var repeat_type: String? = ""
    var repeat_value: String? = ""
    var repeat_value_local: String? = ""
    var repeat_end_type: String? = "never"
    var repeat_end_value: String? = ""
    var is_private: Boolean? = false
    var repeat_task: Boolean? = false
    var all_day: Boolean? = false
    var qb_team_id: String? = ""
    var is_start_team: Boolean? = false
    var start_time_stamp: Long? = null
    var end_time_stamp: Long? = null
    var total_slots: Int? = 0
    var team_name: String? = null
    var startTimeNew:String?=null
    var endTimeNew:String?=null
}