package com.example.sameteam.homeScreen.bottomNavigation.taskModule.model

import com.example.sameteam.authScreens.model.UserModel

class TaskParticipantsModel(
    var id: Int ?= 0,
    var user: UserModel?,
    var response: String? = "pending"
)