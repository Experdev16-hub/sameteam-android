package com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.UserDirectoryCardLayoutBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CreateTeamActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.TeamDetailsActivity
import com.example.sameteam.homeScreen.drawerNavigation.activity.AddEventActivity
import com.google.gson.Gson
import com.quickblox.chat.model.QBChatDialog
import com.quickblox.chat.model.QBDialogType
import java.util.*

class UserDirectoryAdapter(
    val context: Context,
    val items: List<UserModel>,
    val selectedItems: ArrayList<UserModel>?,
    val groupItems: MutableList<QBChatDialog>?
) : RecyclerView.Adapter<UserDirectoryAdapter.DirectoryViewHolder>() {

    private val TAG = "UserDirectoryAdapter"
    companion object {
        var clickMyMethodListener: clickMyMethod? = null

        fun setListener(clickMyMethodListener: clickMyMethod?) {
            // do something
            this.clickMyMethodListener = clickMyMethodListener
        }

    }

    open interface clickMyMethod {
        fun onInviteChatDialogClickMyMethod(participant: QBChatDialog)
        fun onRemoveChatDialogClickMyMethod(participant: QBChatDialog)
    }

    //Implemented in CreateTaskActivity
    interface InvitePeopleListener {
        fun onInvite(participant: UserModel)
        fun onRemove(participant: UserModel)
    }

    interface InviteTeamListener {
        fun onInvite(chatDialog: QBChatDialog)
        fun onRemove(chatDialog: QBChatDialog)
    }

    lateinit var invitePeopleListener: InvitePeopleListener

    lateinit var inviteTeamListener: InviteTeamListener

    fun setclickMyMethodListener(clickMyMethodListener: clickMyMethod) {
        UserDirectoryAdapter?.clickMyMethodListener = clickMyMethodListener
    }

    class DirectoryViewHolder(val binding: UserDirectoryCardLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    var newArraylist: ArrayList<UserModel>

    init {
        Log.d(TAG, "selected: ${selectedItems?.size}")
        Log.d(TAG, "selected: ${Gson().toJson(selectedItems)}")

        Log.d(TAG, "total: ${items?.size}")

        newArraylist = ArrayList(selectedItems)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        return DirectoryViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.user_directory_card_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        if (context is CreateTaskActivity) {
            Log.i("Create Task Activity", "Yes")


            holder.binding.endLayout.visibility = View.VISIBLE
            holder.binding.crossLayout.visibility = View.GONE

            if (!items.isNullOrEmpty()) {
                val item = items[position]
                holder.binding.initials.visibility = View.GONE

                holder.binding.name.text =
                    if (item.id == SharedPrefs.getUser(context)?.id)
                        item.first_name + " " + item.last_name + " (You)"
                    else item.first_name + " " + item.last_name

                holder.binding.email.visibility = View.VISIBLE
                holder.binding.email.text = item.email

                if (newArraylist.any { it.id == item.id }) {
                    holder.binding.btnAdd.visibility = View.GONE
                    holder.binding.btnChecked.visibility = View.VISIBLE
                } else {
                    holder.binding.btnAdd.visibility = View.VISIBLE
                    holder.binding.btnChecked.visibility = View.GONE
                }


                holder.binding.btnAdd.setOnClickListener {
                    holder.binding.btnAdd.visibility = View.GONE
                    holder.binding.btnChecked.visibility = View.VISIBLE
                    invitePeopleListener = context
                    invitePeopleListener.onInvite(item)

                }

                holder.binding.btnChecked.setOnClickListener {
                    holder.binding.btnAdd.visibility = View.VISIBLE
                    holder.binding.btnChecked.visibility = View.GONE
                    invitePeopleListener = context
                    invitePeopleListener.onRemove(item)
                }

                Glide.with(context)
                    .load(item.profile_picture)
                    .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .into(holder.binding.imageView)

            }

            if (!groupItems.isNullOrEmpty()) {
                val qbChatDialog = groupItems[position]

                if (qbChatDialog.type != QBDialogType.PRIVATE) {

                    holder.binding.name.text = qbChatDialog.name

                    if (qbChatDialog.photo.isNullOrBlank()) {
                        holder.binding.initials.visibility = View.VISIBLE
                        holder.binding.imageView.visibility = View.GONE

                        val textArray =
                            qbChatDialog.name.split(" ").toTypedArray().filter { it.isNotBlank() }

                        if (textArray.size < 2) {
                            if (textArray[0].length < 2) holder.binding.initials.text =
                                textArray[0][0].toString().uppercase(Locale.getDefault())
                            else holder.binding.initials.text = textArray[0].substring(0, 2)
                                .uppercase(Locale.getDefault())
                        } else {
                            Log.e("Activity Edit", Gson().toJson(textArray))
                            holder.binding.initials.text =
                                "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
                        }
                    } else {
                        holder.binding.initials.visibility = View.GONE
                        holder.binding.imageView.visibility = View.VISIBLE

                        Glide.with(context)
                            .load(qbChatDialog.photo)
                            .error(R.drawable.profile_photo)
                            .placeholder(R.drawable.profile_photo)
                            .circleCrop()
                            .into(holder.binding.imageView)
                    }

                    holder.binding.btnAdd.setOnClickListener {
                        holder.binding.btnAdd.visibility = View.GONE
                        holder.binding.btnChecked.visibility = View.VISIBLE
                        inviteTeamListener = context
                        inviteTeamListener.onInvite(qbChatDialog)
                        clickMyMethodListener?.onInviteChatDialogClickMyMethod(qbChatDialog)
                    }

                    holder.binding.btnChecked.setOnClickListener {
                        holder.binding.btnAdd.visibility = View.VISIBLE
                        holder.binding.btnChecked.visibility = View.GONE
                        inviteTeamListener = context
                        inviteTeamListener.onRemove(qbChatDialog)
                        clickMyMethodListener?.onRemoveChatDialogClickMyMethod(qbChatDialog)

                    }
                }
            }


        } else if (context is CreateTeamActivity) {
            Log.i("Create Team Activity", "Yes")

            holder.binding.endLayout.visibility = View.VISIBLE
            holder.binding.crossLayout.visibility = View.GONE
            holder.binding.initials.visibility = View.GONE

            val item = items?.get(position)

            if (item != null) {


                holder.binding.name.text = item.first_name + " " + item.last_name
                holder.binding.email.visibility = View.VISIBLE
                holder.binding.email.text = item.email

                if (newArraylist.any { it.id == item.id }) {
                    holder.binding.btnAdd.visibility = View.GONE
                    holder.binding.btnChecked.visibility = View.VISIBLE
                } else {
                    holder.binding.btnAdd.visibility = View.VISIBLE
                    holder.binding.btnChecked.visibility = View.GONE
                }


                Glide.with(context)
                    .load(item.profile_picture)
                    .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .into(holder.binding.imageView)

                holder.binding.btnAdd.setOnClickListener {
                    holder.binding.btnAdd.visibility = View.GONE
                    holder.binding.btnChecked.visibility = View.VISIBLE
                    invitePeopleListener = context
                    invitePeopleListener.onInvite(item)
                }

                holder.binding.btnChecked.setOnClickListener {
                    holder.binding.btnAdd.visibility = View.VISIBLE
                    holder.binding.btnChecked.visibility = View.GONE
                    invitePeopleListener = context
                    invitePeopleListener.onRemove(item)
                }
            }


        } else if (context is TeamDetailsActivity) {
            Log.i("Team Detail Activity", "Yes")
            holder.binding.crossLayout.visibility = View.GONE
            holder.binding.btnChecked.visibility = View.GONE
            holder.binding.initials.visibility = View.GONE


            val item = items?.get(position)


            if (newArraylist.any { it.id == item?.id }) {
                holder.binding.endLayout.visibility = View.VISIBLE

                holder.binding.btnAdd.visibility = View.VISIBLE
                holder.binding.btnAdd.text = context.getString(R.string.admin)
            } else {
                holder.binding.endLayout.visibility = View.GONE
                holder.binding.btnAdd.visibility = View.VISIBLE
            }

            if (item != null) {
                holder.binding.name.text = if (item.id == SharedPrefs.getUser(context)?.id) {
                    item.first_name + " " + item.last_name + " (You)"
                } else item.first_name + " " + item.last_name

                Glide.with(context)
                    .load(item.profile_picture)
                    .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .into(holder.binding.imageView)
            }
        } else if (context is HomeActivity) {
            Log.i("Home Activity", "Yes")

            holder.binding.crossLayout.visibility = View.GONE
            holder.binding.btnChecked.visibility = View.GONE
            holder.binding.initials.visibility = View.GONE
            holder.binding.endLayout.visibility = View.GONE

            val item = items?.get(position)

            if (item != null) {
                holder.binding.name.text = item.first_name + " " + item.last_name

                Glide.with(context)
                    .load(item.profile_picture)
                    .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                    .into(holder.binding.imageView)
            }

            if (item != null) {
                when (item.response) {
//            Constants.PENDING -> {
//                Glide.with(view.context)
//                    .asDrawable()
//                    .load(ContextCompat.getDrawable(view.context, R.drawable.ic_time_left))
//                    .into(view)
//            }
//            else-> { Glide.with(view.context)
//                .asDrawable()
//                .load(ContextCompat.getDrawable(view.context, R.drawable.ic_check_color))
//                .into(view)
//            }
                    Constants.PENDING -> {

                        Glide.with(context)
                            .asDrawable()
                            .load(ContextCompat.getDrawable(context, R.drawable.ic_time_left))
                            .into(holder.binding.status)
                    }

                    Constants.ACCEPTED -> {
                        Glide.with(context)
                            .asDrawable()
                            .load(ContextCompat.getDrawable(context, R.drawable.ic_check_color))
                            .into(holder.binding.status)
                    }

                    Constants.DECLINED -> {
                        Glide.with(context)
                            .asDrawable()
                            .load(ContextCompat.getDrawable(context, R.drawable.ic_cancel_red))
                            .into(holder.binding.status)
                    }

                    else -> {
                        Glide.with(context)
                            .asDrawable()
                            .load(ContextCompat.getDrawable(context, R.drawable.ic_time_left))
                            .into(holder.binding.status)
                    }
                }
            } else if (context is AddEventActivity) {
                Log.i("Add Event Activity", "Yes")
                holder.binding.endLayout.visibility = View.VISIBLE
                holder.binding.crossLayout.visibility = View.GONE
                holder.binding.initials.visibility = View.GONE

                if (!items.isNullOrEmpty()) {

                    val item = items[position]

                    holder.binding.name.text = if (item.id == SharedPrefs.getUser(context)?.id) {
                        item.first_name + " " + item.last_name + " (You)"
                    } else item.first_name + " " + item.last_name

                    // holder.binding.name.text = item.first_name + " " + item.last_name
                    holder.binding.email.visibility = View.VISIBLE
                    holder.binding.email.text = item.email

                    if (newArraylist.any { it.id == item.id }) {
                        holder.binding.btnAdd.visibility = View.GONE
                        holder.binding.btnChecked.visibility = View.VISIBLE
                    } else {
                        holder.binding.btnAdd.visibility = View.VISIBLE
                        holder.binding.btnChecked.visibility = View.GONE
                    }

                    Glide.with(context)
                        .load(item.profile_picture)
                        .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .into(holder.binding.imageView)

                    holder.binding.btnAdd.setOnClickListener {
                        holder.binding.btnAdd.visibility = View.GONE
                        holder.binding.btnChecked.visibility = View.VISIBLE
                        invitePeopleListener = context
                        invitePeopleListener.onInvite(item)

                    }

                    holder.binding.btnChecked.setOnClickListener {
                        holder.binding.btnAdd.visibility = View.VISIBLE
                        holder.binding.btnChecked.visibility = View.GONE
                        invitePeopleListener = context
                        invitePeopleListener.onRemove(item)
                    }
                }

                if (!groupItems.isNullOrEmpty()) {
                    val qbChatDialog = groupItems[position]

                    if (qbChatDialog.type != QBDialogType.PRIVATE) {

                        holder.binding.name.text = qbChatDialog.name

                        if (qbChatDialog.photo.isNullOrBlank()) {
                            holder.binding.initials.visibility = View.VISIBLE
                            holder.binding.imageView.visibility = View.GONE

                            val textArray = qbChatDialog.name.split(" ").toTypedArray()
                                .filter { it.isNotBlank() }
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
                            holder.binding.imageView.visibility = View.VISIBLE

                            Glide.with(context)
                                .load(qbChatDialog.photo)
                                .error(R.drawable.profile_photo)
                                .placeholder(R.drawable.profile_photo)
                                .circleCrop()
                                .into(holder.binding.imageView)
                        }

                        holder.binding.btnAdd.setOnClickListener {
                            holder.binding.btnAdd.visibility = View.GONE
                            holder.binding.btnChecked.visibility = View.VISIBLE
                            inviteTeamListener = context
                            inviteTeamListener.onInvite(qbChatDialog)
                            clickMyMethodListener?.onInviteChatDialogClickMyMethod(qbChatDialog)

                        }

                        holder.binding.btnChecked.setOnClickListener {
                            holder.binding.btnAdd.visibility = View.VISIBLE
                            holder.binding.btnChecked.visibility = View.GONE
                            inviteTeamListener = context
                            inviteTeamListener.onRemove(qbChatDialog)
                            clickMyMethodListener?.onRemoveChatDialogClickMyMethod(qbChatDialog)

                        }
                    }
                }
            }
        }
    }
    override fun getItemCount(): Int {
        return if (items?.size != 0 && groupItems?.size == 0) items?.size ?: 0
        else if (groupItems?.size != 0 && items?.size == 0) groupItems?.size ?: 0
        else 0
    }
}