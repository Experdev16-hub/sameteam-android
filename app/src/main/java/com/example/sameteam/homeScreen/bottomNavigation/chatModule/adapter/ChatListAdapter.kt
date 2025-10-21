package com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.databinding.ChatListLayoutBinding
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.ChatActivity
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.google.gson.Gson
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBDialogType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatListAdapter(var context: Context, var dialogs: List<QBChatDialog>) :
    RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private val TAG = "DialogsAdapter"
    private var onlineUsers = ArrayList<Int>()

    class ViewHolder(val binding: ChatListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.chat_list_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = dialogs[position]
        onlineUsers = SharedPrefs.getOnlineUserIds(MyApplication.getInstance())

        holder.binding.message.text = prepareTextLastMessage(item)
        holder.binding.time.text = getDialogLastMessageTime(item.lastMessageDateSent)
        if (getUnreadMsgCount(item) == 0)
            holder.binding.count.visibility = View.GONE
        else {
            holder.binding.count.visibility = View.VISIBLE
            holder.binding.count.text = getUnreadMsgCount(item).toString()
        }

        holder.binding.parent.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            val gson = Gson()
            intent.putExtra("chatDialog", gson.toJson(item))
            context.startActivity(intent)
        }

        if (item.type == QBDialogType.PRIVATE) {
            val opponentId = item.recipientId
            val user = QbUsersDbManager.getUserById(opponentId)

            if (user != null && !user.customData.isNullOrBlank()) {
                val newUser = Utils.getUserFromQBUser(user.customData)

                holder.binding.name.text = user.fullName
                holder.binding.initials.visibility = View.GONE
                holder.binding.profileImage.visibility = View.VISIBLE

                Glide.with(context)
                    .load(newUser.profile_picture)
                    .error(R.drawable.profile_photo)
                    .placeholder(R.drawable.profile_photo)
                    .circleCrop()
                    .into(holder.binding.profileImage)
            }


            if (onlineUsers.contains(opponentId)) {
                holder.binding.onlineStatus.visibility = View.VISIBLE
            } else {
                holder.binding.onlineStatus.visibility = View.GONE
            }
        } else if (item.type == QBDialogType.GROUP) {
            holder.binding.name.text = item.name
            holder.binding.onlineStatus.visibility = View.GONE

            if (item.photo.isNullOrBlank()) {
                holder.binding.initials.visibility = View.VISIBLE
                holder.binding.profileImage.visibility = View.GONE

                val textArray = item.name.split(" ").toTypedArray().filter { it.isNotBlank() }
                if (textArray.size < 2) {
                    if (textArray[0].length < 2) holder.binding.initials.text =
                        textArray[0][0].toString().uppercase(Locale.getDefault())
                    else holder.binding.initials.text = textArray[0].substring(0, 2)
                        .uppercase(Locale.getDefault())
                } else {
                    holder.binding.initials.text =
                        "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
                }
            } else {
                holder.binding.initials.visibility = View.GONE
                holder.binding.profileImage.visibility = View.VISIBLE

                Glide.with(context)
                    .load(item.photo)
                    .error(R.drawable.profile_photo)
                    .placeholder(R.drawable.profile_photo)
                    .circleCrop()
                    .into(holder.binding.profileImage)
            }

        }

    }

    override fun getItemCount(): Int {
        return dialogs.size
    }

    private fun getUnreadMsgCount(chatDialog: QBChatDialog): Int {
        val unreadMessageCount = chatDialog.unreadMessageCount
        return unreadMessageCount ?: 0
    }

    private fun isLastMessageAttachment(dialog: QBChatDialog): Boolean {
        val lastMessage = dialog.lastMessage
        val lastMessageSenderId = dialog.lastMessageUserId
        return TextUtils.isEmpty(lastMessage) && lastMessageSenderId != null
    }

    private fun prepareTextLastMessage(chatDialog: QBChatDialog): String {
        var lastMessage = ""
        if (isLastMessageAttachment(chatDialog)) {
            lastMessage = context.getString(R.string.chat_attachment)
        } else {
            chatDialog.lastMessage?.let {
                lastMessage = it
            }
        }
        return lastMessage
    }

fun updateList(newData: List<QBChatDialog>) {
    try {
        dialogs = newData
        notifyDataSetChanged()
    } catch (e: Exception) {
        Log.e(TAG, "Error updating chat list: ${e.message}")
    }
}


    private fun getDialogLastMessageTime(seconds: Long): String {
        Log.d(TAG, "getDialogLastMessageTime: $seconds")
        val timeInMillis = seconds * 1000
        val msgTime = Calendar.getInstance()
        msgTime.timeInMillis = timeInMillis

        if (timeInMillis == 0L) {
            return ""
        }

        val now = Calendar.getInstance()
        val timeFormatToday = SimpleDateFormat("hh:mm aa", Locale.ENGLISH)
        val dateFormatThisYear = SimpleDateFormat("d MMM", Locale.ENGLISH)
        val lastYearFormat = SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)

        if (now.get(Calendar.DATE) == msgTime.get(Calendar.DATE) && now.get(Calendar.YEAR) == msgTime.get(
                Calendar.YEAR
            )
        ) {
            return timeFormatToday.format(Date(timeInMillis))
        } else if (now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1 && now.get(
                Calendar.YEAR
            ) == msgTime.get(Calendar.YEAR)
        ) {
            return context.getString(R.string.yesterday)
        } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)) {
            return dateFormatThisYear.format(Date(timeInMillis))
        } else {
            return lastYearFormat.format(Date(timeInMillis))
        }
    }

}
