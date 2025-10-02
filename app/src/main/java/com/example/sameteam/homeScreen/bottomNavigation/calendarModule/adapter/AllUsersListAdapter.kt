package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.UserDirectoryCardLayoutBinding

class AllUsersListAdapter(val context: Context) :
    PagingDataAdapter<UserModel, AllUsersListAdapter.MyViewHolder>(DataDifferentiator) {

    object DataDifferentiator : DiffUtil.ItemCallback<UserModel>() {
        override fun areItemsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: UserModel, newItem: UserModel): Boolean {
            return oldItem == newItem
        }
    }

    class MyViewHolder(val binding: UserDirectoryCardLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.binding.endLayout.visibility = View.GONE
        holder.binding.crossLayout.visibility = View.GONE

        val item = getItem(position)

        Glide.with(context)
            .load(item?.profile_picture)
            .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
            .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
            .into(holder.binding.imageView)

        holder.binding.name.text = "${item?.first_name} ${item?.last_name}"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.user_directory_card_layout,
                parent,
                false
            )
        )
    }

}