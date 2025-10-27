package com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import com.example.sameteam.R
import com.example.sameteam.databinding.RowImageBinding
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.OverlapImageModel
import com.mindinventory.overlaprecylcerview.adapters.OverlapRecyclerViewAdapter
import com.mindinventory.overlaprecylcerview.utils.TextDrawable

/**
 * This is to overlap users profile image in event card layout
 */
class OverlapAdapter(
    overlapLimit: Int,
    overlapWidthInPercentage: Int
) : OverlapRecyclerViewAdapter<OverlapImageModel, OverlapAdapter.CustomViewHolder>(
    overlapLimit,
    overlapWidthInPercentage
) {

    lateinit var context: Context

    override fun createItemViewHolder(parent: ViewGroup): CustomViewHolder {
        context = parent.context
        return CustomViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.row_image,
                parent,
                false
            )
        )
    }

    override fun bindItemViewHolder(holder: CustomViewHolder, position: Int) {
        val currentImageModel = getVisibleItemAt(position)!!
        //----bind data to view
        holder.bind(currentImageModel)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        bindItemViewHolder(holder, position)
    }

    override fun getItemCount() = visibleItems.size

    inner class CustomViewHolder(val binding: RowImageBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * bind model data to item
         */
        fun bind(overlapImageModel: OverlapImageModel) {
            if (isLastVisibleItemItem(absoluteAdapterPosition)) {
                //----set text drawable to show count on last imageview
                val text = "+" + (notVisibleItems.size + 1).toString()
                val drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .width(90)
                    .height(90)
                    .endConfig()
                    .buildRound(text, Color.parseColor("#2F88D6"))
                binding.imageView.setImageDrawable(drawable)
            } else {
                if (overlapImageModel.imageUrl.isNullOrBlank()) {
                    Glide.with(binding.imageView.context)
                        .load(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .into(binding.imageView)
                } else {
                    Glide.with(binding.imageView.context)
                        .load(overlapImageModel.imageUrl)
                        .apply(RequestOptions.circleCropTransform().priority(Priority.HIGH))
                        .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .into(binding.imageView)
                }
            }
        }
    }
}
