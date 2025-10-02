package com.example.sameteam.helper

import android.Manifest
import com.example.sameteam.BuildConfig

object Constants {

    /**
     * Change QBEnvironment in Manifest file also
     */
    val QBEnvironment =
        if (BuildConfig.FLAVOR == "client") com.quickblox.messages.model.QBEnvironment.PRODUCTION
        else com.quickblox.messages.model.QBEnvironment.DEVELOPMENT

    /* val QBEnvironment =
         if (BuildConfig.FLAVOR == "client") com.quickblox.messages.model.QBEnvironment.DEVELOPMENT
         else com.quickblox.messages.model.QBEnvironment.DEVELOPMENT
 */
    const val VISIBLE = "visible"
    const val HIDE = "hide"
    const val SHOW_PROGRESS = "showProgress"
    const val HIDE_PROGRESS = "hideProgress"
    const val NAVIGATE = "navigate"
    const val FORCE_LOGOUT = "ForceLogout"
    const val DELETE_ACCOUNT = "DeleteAccount"

    const val NOTIFICATION_VIEW_TYPE_1 = 1  // Event Invitation
    const val NOTIFICATION_VIEW_TYPE_2 = 2  // Task Invitation
    const val NOTIFICATION_VIEW_TYPE_3 = 3  // Task Invitation Accept
    const val NOTIFICATION_VIEW_TYPE_4 = 4  // Task Invitation Reject
    const val NOTIFICATION_VIEW_TYPE_5 = 5  // Task Reminder

    const val SPECIFIC_DATE = "SpecificDate"

    const val START_DATE = "StartDate"
    const val END_DATE = "EndDate"

    const val CAMERA_INTENT = "cameraIntent"
    const val GALLERY_INTENT = "galleryIntent"

    const val ACCEPTED = "accepted"
    const val PENDING = "pending"
    const val DECLINED = "declined"

    const val DEFAULT_PAGE_SIZE = 10

    const val CHAT_RECEIVED_VIEW_TYPE = 3
    const val CHAT_SENT_VIEW_TYPE = 4
    const val TYPE_ATTACH_LEFT = 5
    const val TYPE_ATTACH_RIGHT = 6
    const val CUSTOM_VIEW_TYPE = -1
    const val TYPE_NOTIFICATION_CENTER = 7
    const val FILE_DOWNLOAD_ATTEMPS_COUNT = 2

    const val EXTRA_FCM_MESSAGE = "message"
    const val ACTION_NEW_FCM_EVENT = "new-push-event"
    const val EMPTY_FCM_MESSAGE = "empty message"
    const val SHOW_CHAT_BADGE = "showChatBadge"
    const val SHOW_NOTIFICATION_BADGE = "showNotificationBadge"
    const val DELETE_TASK = "deleteTask"
    const val COMPLETE_TASK = "completeTask"


    const val IS_IN_BACKGROUND = "is_in_background"
    const val EXTRA_DIALOG_ID = "dialogId"
    const val SEND_TYPING_STATUS_DELAY: Long = 3000L
    const val TYPING_STATUS_DELAY = 2000L
    const val TYPING_STATUS_INACTIVITY_DELAY = 10000L
    const val EXTRA_IS_NEW_DIALOG = "isNewDialog"

    const val JAN = "Jan"
    const val FEB = "Feb"
    const val MAR = "Mar"
    const val APR = "Apr"
    const val MAY = "May"
    const val JUN = "Jun"
    const val JUL = "Jul"
    const val AUG = "Aug"
    const val SEP = "Sep"
    const val OCT = "Oct"
    const val NOV = "Nov"
    const val DEC = "Dec"

    const val UNAUTHORIZED = 401

    const val ORDER_RULE = "order"
    const val ORDER_VALUE_UPDATED_AT = "desc string updated_at"
    const val USERS_PAGE_SIZE = 100
    const val PLAY_SERVICES_REQUEST_CODE = 9000


    const val MAX_MESSAGE_SYMBOLS_LENGTH = 1000

    val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    const val MAX_OPPONENTS_COUNT = 6

    const val EXTRA_LOGIN_RESULT = "login_result"

    const val EXTRA_LOGIN_ERROR_MESSAGE = "login_error_message"

    const val EXTRA_LOGIN_RESULT_CODE = 1002

    const val EXTRA_IS_INCOMING_CALL = "conversation_reason"
    const val EXTRA_INCOMING_GROUP_NAME = "incoming_group_name"

    const val MAX_LOGIN_LENGTH = 15

    const val MAX_FULLNAME_LENGTH = 20

    const val OVERLAY_PERMISSION_CHECKED_KEY = "overlay_checked"
    const val MI_OVERLAY_PERMISSION_CHECKED_KEY = "mi_overlay_checked"
    const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1764
    const val ERROR_LOGIN_ALREADY_TAKEN_HTTP_STATUS = 422

    const val PLAN_STATUS = "PlanStatus"

}