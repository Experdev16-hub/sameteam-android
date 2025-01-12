package com.example.sameteam.retrofit

import com.example.sameteam.BuildConfig

class API {

    companion object {

        // BASE URL is in Retrofit.kt class
        val BASE_URL =
            if (BuildConfig.FLAVOR == "client") "https://api.sameteam.app/sameteam-api/"
            else ""



        //Login
        const val LOGIN = "user/login"

        //Register
        const val REGISTER = "user/register"

        //For Changing TOKEN
        const val REFRESH_TOKEN = "user/refresh-token"

        //Logout
        const val LOGOUT = "user/logout"

        //Fetch & Edit User
        const val USER = "user"

        //Forgot Password
        const val FORGOT_PASSWORD = "user/forgot-password"

        //Change Password
        const val CHANGE_PASSWORD = "user/change-password"

        //Create, Edit & Delete Task, Get Task by ID
        const val CREATE_TASK = "task"

        //Get events and participants for create task
        const val EVENTS_AND_PARTICIPANTS = "task/constants"

        //       Changing task status
        const val CHANGE_TASK_STATUS = "task/complete"

        //Get my events
        const val MY_EVENTS = "user/getMyEvent"

        //Get all tasks
        const val ALL_TASKS = "task/tasks"

        //record participant response for task
        const val PARTICIPANT_RESPONSE = "task/participant-response"

        //List of users
        const val USER_DIRECTORY = "user/directory"

        //Show tasks on calendar view
        const val GET_MONTH_DATES = "task/getAllDates"

        //Change Notification Status
        const val CHANGE_NOTIFICATION_STATUS = "user/change-notification-status"

        //Notification Data API
        const val GET_NOTIFICATIONS = "user/getNotifications"

        //Delete User Account
        const val DELETE_ACCOUNT = "user/deleteUser"

        const val GET_USER_LIST = "sub-admin/getUserList"

        const val ADD_EVENT = "sub-admin/addevent"

        const val GET_EVENT_BY_ID = "sub-admin/getEventById"

        const val EDIT_EVENT = "sub-admin/editEvent"

        const val SIGN_UP_FOR_TASK = "task/signUpInTask"

        const val GET_PARTICIPANT_ID_LIST = "user/getParticipantIdList"

        const val ADD_USER_CONTACT = "user/addUserContact"

        const val EDIT_QB_TEAM = "task/editQBTeam"

    }
}