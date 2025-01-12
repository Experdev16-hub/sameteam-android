package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model

import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BaseResponse
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantsModel

class TaskDetailsResponseModel : BaseResponse<TaskDetailsResponseModel.Data>() {

    data class Data(
        var id: Int = 0,
        var name: String? = null,
        var description: String? = null,
        var event: EventModel? = null,
        var user: UserModel = UserModel(),
        var firstName: String? = null,
        var lastName: String? = null,
        var location: String? = null,
        var completed: Boolean? = false,
        var status: String? = null,
        var remind_me: ArrayList<RemindMeModel> = ArrayList(),
        var start_date: String? = null,
        var start_time: String? = "",
        var end_time: String? = "",
        var repeat_type: String? = "",
        var repeat_value: String? = "",
        var repeat_value_local: String? = "",
        var repeat_end_type: String? = "never",
        var repeat_end_value: String? = "",
        var is_private: Boolean? = false,
        var repeat_task: Boolean? = false,
        var all_day: Boolean? = false,
        var image_url: String? = null,
        var task_participants: ArrayList<TaskParticipantsModel> = ArrayList(),
        var qb_team_id: String? = "",
        var is_start_team: Boolean? = false,

        var start_time_stamp: Long? = null,
        var end_time_stamp: Long? = null,
        var total_slots: Int? = null,
        var available_slots: Int? = null,
        var slot_available: Boolean? = false,
        var sign_up_user: Boolean? = false,
        var team_name: String? = null,

    )
}