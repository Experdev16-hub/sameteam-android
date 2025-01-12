package com.example.sameteam.helper

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.google.gson.Gson

object BindingAdapter {

    @BindingAdapter("loadProfilePicture")
    @JvmStatic
    fun loadImage(view: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            Glide.with(view.context)
                .asDrawable()
                .load(ContextCompat.getDrawable(view.context, R.drawable.profile_photo))
                .into(view)
        } else {
            Glide.with(view.context)
                .load(url)
                .error(ContextCompat.getDrawable(view.context, R.drawable.profile_photo))
                .placeholder(ContextCompat.getDrawable(view.context, R.drawable.profile_photo))
                .into(view)
        }
    }

    @BindingAdapter("loadQbDialogPhoto")
    @JvmStatic
    fun loadQBDialogPhoto(view: ImageView, url: String?) {
        if (!url.isNullOrBlank()) {
            Glide.with(view.context)
                .load(url)
                .error(ContextCompat.getDrawable(view.context, R.drawable.image_placeholder))
                .placeholder(
                    ContextCompat.getDrawable(
                        view.context,
                        R.drawable.image_placeholder
                    )
                )
                .into(view)
        }
    }

    @BindingAdapter("loadUserResponse")
    @JvmStatic
    fun loadResponse(view: ImageView, response: String? = null) {

        if (response != null) {


            when (response) {
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
                    Glide.with(view.context)
                        .asDrawable()
                        .load(ContextCompat.getDrawable(view.context, R.drawable.ic_time_left))
                        .into(view)
                }

                Constants.ACCEPTED -> {
                    Glide.with(view.context)
                        .asDrawable()
                        .load(ContextCompat.getDrawable(view.context, R.drawable.ic_check_color))
                        .into(view)
                }

                Constants.DECLINED -> {
                    Glide.with(view.context)
                        .asDrawable()
                        .load(ContextCompat.getDrawable(view.context, R.drawable.ic_cancel_red))
                        .into(view)
                }

                else -> {
                    Glide.with(view.context)
                        .asDrawable()
                        .load(ContextCompat.getDrawable(view.context, R.drawable.ic_time_left))
                        .into(view)
                }
            }
        }


    }

    @BindingAdapter("hideIfAlreadyAdded")
    @JvmStatic
    fun hideIfAlreadyAdded(view: TextView, isTeamContact: Boolean?) {
        view.visibility = if (isTeamContact == true) View.GONE else View.VISIBLE
    }

    @BindingAdapter("participantName")
    @JvmStatic
    fun participantName(view: TextView, user: UserModel?) {
        view.text = if (user?.id == SharedPrefs.getUser(view.context)?.id)
            user?.first_name + " " + user?.last_name + " (You)"
        else user?.first_name + " " + user?.last_name
    }
}