package com.example.sameteam.authScreens.model

import com.example.sameteam.base.BaseResponse
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.CompanyModel

class LoginResponseModel : BaseResponse<LoginResponseModel.User>() {

    inner class User(
        var id: Int? = 0,
        var company: CompanyModel? = null,
        var last_name: String? = null,
        var first_name: String? = null,
        var email: String? = null,
        var mobile_number: String? = null,
        var title: String? = null,
        var profile_picture: String? = null,
        var is_approved: Boolean? = false,
        var notification_status: String? = null,
        var status: String? = null,
        var token: String? = null,
        var refresh_token: String? = null,
        var token_expiry: Long? = 0,
        var platform_type: String? = null,
        var country_code: String? = null,
        var plan_upgrade: Boolean? = false,
        var isTeamContact:Boolean?=false //Add user from team contact
    )

}
