package com.example.sameteam.authScreens.model

import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.CompanyModel


class UserModel(
    var id: Int? = 0,
    var company: CompanyModel? = null,
    var last_name: String? = null,
    var first_name: String? = null,
    var email: String? = null,
    var country_code: String? = null,
    var mobile_number: String? = null,
    var title: String? = null,
    var profile_picture: String? = null,
    var is_approved: Boolean? = false,
    var notification_status: String? = null,
    var platform_type: String? = null,
    var status: String? = null,
    var plan_upgrade: Boolean? = false,
    var participant_user: Boolean? = false,
    var response:String?=null
)