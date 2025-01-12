package com.example.sameteam.quickBlox.fragments

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CallActivity
import com.example.sameteam.quickBlox.base.BaseConversationFragment
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.quickblox.core.helper.StringifyArrayList
import com.quickblox.users.model.QBUser
import com.quickblox.videochat.webrtc.AppRTCAudioManager
import java.util.*

const val SPEAKER_ENABLED = "is_speaker_enabled"

class AudioConversationFragment  : BaseConversationFragment(), CallActivity.OnChangeAudioDevice {

    private lateinit var audioSwitchToggleButton: ToggleButton
    private lateinit var alsoOnCallText: TextView
    private lateinit var firstOpponentNameTextView: TextView
    private lateinit var otherOpponentsTextView: TextView
    private lateinit var firstOpponentAvatarImageView: ImageView

    override fun onStart() {
        super.onStart()
        conversationFragmentCallback?.addOnChangeAudioDeviceListener(this)
    }

    override fun configureOutgoingScreen() {
        val context: Context = activity as Context
        outgoingOpponentsRelativeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.lightBlue))
        allOpponentsTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color_outgoing_opponents_names_audio_call))
        ringingTextView.setTextColor(ContextCompat.getColor(context, R.color.text_color_call_type))
    }

    override fun configureToolbar() {
        val context: Context = activity as Context
        toolbar.visibility = View.VISIBLE
        toolbar.setBackgroundColor(ContextCompat.getColor(context, R.color.lightBlue))
        toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.black))
        toolbar.setSubtitleTextColor(ContextCompat.getColor(context, R.color.black))
    }

    override fun configureActionBar() {
        if(currentUser.fullName.isNullOrBlank() || currentUser.fullName == "null"){
            actionBar.subtitle = ""
        }
        else{
            actionBar.subtitle = String.format(getString(R.string.subtitle_text_logged_in_as), currentUser.fullName)
        }
    }

    override fun initViews(view: View?) {
        super.initViews(view)
        if (view == null) {
            return
        }
        timerCallText = view.findViewById(R.id.timer_call)

        firstOpponentAvatarImageView = view.findViewById(R.id.image_caller_avatar)

        alsoOnCallText = view.findViewById(R.id.text_also_on_call)
        setVisibilityAlsoOnCallTextView()

        firstOpponentNameTextView = view.findViewById(R.id.text_caller_name)

        if(groupName.isNotBlank())
            firstOpponentNameTextView.text = groupName
        else
            firstOpponentNameTextView.text = opponents[0].fullName


        otherOpponentsTextView = view.findViewById(R.id.text_other_inc_users)
        otherOpponentsTextView.text = getOtherOpponentsNames()

        audioSwitchToggleButton = view.findViewById(R.id.toggle_speaker)
        audioSwitchToggleButton.visibility = View.VISIBLE
        audioSwitchToggleButton.isChecked = SharedPrefsHelper.get(SPEAKER_ENABLED, true)
        actionButtonsEnabled(true)

        if (conversationFragmentCallback?.isCallState() == true) {
            onCallStarted()
        }
    }

    private fun setVisibilityAlsoOnCallTextView() {
        if (opponents.size < 2) {
            alsoOnCallText.visibility = View.INVISIBLE

            val newUser = Utils.getUserFromQBUser(opponents[0].customData)

            Glide.with(requireContext())
                .load(newUser.profile_picture)
                .error(R.drawable.profile_photo)
                .placeholder(R.drawable.profile_photo)
                .circleCrop()
                .into(firstOpponentAvatarImageView)

        }
    }

    private fun getOtherOpponentsNames(): String {
        val otherOpponents = ArrayList<QBUser>()
        otherOpponents.addAll(opponents)

        if(otherOpponents.size < 2)
            return ""

        try {
            otherOpponents.remove(currentUser)
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return makeStringFromUsersFullNames(otherOpponents)
    }

    private fun makeStringFromUsersFullNames(allUsers: ArrayList<QBUser>): String {
        val name = mutableSetOf<String>()
        val newUsers = ArrayList<QBUser>()

        for (user in allUsers){
            if(!name.contains(user.fullName)){
                newUsers.add(user)
                name.add(user.fullName)
            }
        }

        val usersNames = StringifyArrayList<String>()
        for (user in newUsers) {
            if (user.fullName != null) {
                usersNames.add(user.fullName)
            }
//            else if (user.id != null) {
//                usersNames.add(user.id.toString())
//            }
        }
        return usersNames.itemsAsString.replace(",", ", ")
    }

    override fun onStop() {
        super.onStop()
        conversationFragmentCallback?.removeOnChangeAudioDeviceListener(this)
    }

    override fun initButtonsListener() {
        super.initButtonsListener()
        audioSwitchToggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            SharedPrefsHelper.save(SPEAKER_ENABLED, isChecked)
            conversationFragmentCallback?.onSwitchAudio()
        }
    }

    override fun actionButtonsEnabled(inability: Boolean) {
        super.actionButtonsEnabled(inability)
        audioSwitchToggleButton.isActivated = inability
    }

    override fun getFragmentLayout(): Int {
        return R.layout.fragment_audio_conversation
    }

    override fun onOpponentsListUpdated(newUsers: ArrayList<QBUser>) {
        super.onOpponentsListUpdated(newUsers)
        if(groupName.isNotBlank()){
            firstOpponentNameTextView.text = groupName
        }
        else{
            firstOpponentNameTextView.text = opponents[0].fullName
        }
        otherOpponentsTextView.text = getOtherOpponentsNames()
    }

    override fun onCallTimeUpdate(time: String) {
        timerCallText.text = time
    }

    override fun audioDeviceChanged(newAudioDevice: AppRTCAudioManager.AudioDevice) {
        audioSwitchToggleButton.isChecked = newAudioDevice != AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
    }
}