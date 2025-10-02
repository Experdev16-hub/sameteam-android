package com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter;

import android.content.Context;
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.databinding.ChatListLayoutBinding
import com.example.sameteam.databinding.TeamListLayoutBinding
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.CreateTeamActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.TeamDetailsActivity
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.google.gson.Gson

import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBDialogType
import com.quickblox.users.model.QBUser

import java.util.*

class TeamListAdapter(
    var context: Context,
    var dialogs: List<QBChatDialog>
) : RecyclerView.Adapter<TeamListAdapter.ViewHolder>() {

    private var onlineUsers = ArrayList<Int>()

    class ViewHolder(val binding: TeamListLayoutBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamListAdapter.ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.team_list_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: TeamListAdapter.ViewHolder, position: Int) {
        var currentUser = QBUser()

        val item = dialogs[position]

        if (ChatHelper.getCurrentUser() != null) {
            currentUser = ChatHelper.getCurrentUser()!!
        } else {
            Log.e("TeamListAdapter", "Finishing TeamListAdapter. Current user is null")
        }
        Log.i("Dialogs", "Length $item")


        if (item.type == QBDialogType.GROUP) {
            Log.i("Dialog Type", "Type" + item.type)

            holder.binding.name.text = item.name

            if (item.photo.isNullOrBlank()) {
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
            if (item.userId == currentUser.id
            ) {
                holder.binding.btnEdit.visibility = View.VISIBLE

                holder.binding.btnEdit.setOnClickListener {
                    val intent = Intent(context, CreateTeamActivity::class.java)
                    val gson = Gson()
                    intent.putExtra("editChatDialog", gson.toJson(item))
                    context.startActivity(intent)
                }
            } else {
                holder.binding.btnEdit.visibility = View.GONE
            }

            holder.binding.btnInfo.setOnClickListener {
                val intent = Intent(context, TeamDetailsActivity::class.java)
                val gson = Gson()
                intent.putExtra("chatDialog", gson.toJson(item))
                context.startActivity(intent)
            }

        }

    }

    override fun getItemCount(): Int {
        return dialogs.size
    }
}
