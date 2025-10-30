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
import com.mindinventory.overlaprecylcerview.listeners.OverlapRecyclerViewClickListener
import com.mindinventory.overlaprecylcerview.utils.TextDrawable

/**
 * Custom overlap adapter to replace the buggy MindInventory library
 * This shows overlapping profile images with a count on the last item
 */
class OverlapAdapter(
    private val overlapLimit: Int,
    private val overlapWidthInPercentage: Int
) : RecyclerView.Adapter<OverlapAdapter.CustomViewHolder>() {

    private var visibleItems = mutableListOf<OverlapImageModel>()
    private var notVisibleItems = mutableListOf<OverlapImageModel>()
    private lateinit var context: Context
    private var clickListener: OverlapRecyclerViewClickListener? = null

    /**
     * Set the items to display with overlap logic
     */
    fun setItems(allItems: List<OverlapImageModel>) {
        visibleItems = if (allItems.size > overlapLimit) {
            allItems.take(overlapLimit).toMutableList()
        } else {
            allItems.toMutableList()
        }
        notVisibleItems = if (allItems.size > overlapLimit) {
            allItems.drop(overlapLimit).toMutableList()
        } else {
            mutableListOf()
        }
        notifyDataSetChanged()
    }

    // ADD THIS METHOD: This is called from TaskListAdapter
    fun updateData(newItems: List<OverlapImageModel>) {
        setItems(newItems)
    }

    // ADD THIS METHOD: This is called from TaskListAdapter
    fun setOverlapRecyclerViewClickListener(listener: OverlapRecyclerViewClickListener) {
        this.clickListener = listener
    }

    // ADD THIS METHOD: This is called from TaskListAdapter
    fun getItemDecoration(): RecyclerView.ItemDecoration {
        // Return a simple item decoration or create a custom one if needed
        return object : RecyclerView.ItemDecoration() {
            // You can add custom decoration logic here if needed
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        context = parent.context
        val binding = DataBindingUtil.inflate<RowImageBinding>(
            LayoutInflater.from(context),
            R.layout.row_image,
            parent,
            false
        )
        
        // Apply overlap by setting negative margin
        val layoutParams = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
        layoutParams?.marginStart = (-overlapWidthInPercentage).coerceAtMost(0)
        binding.root.layoutParams = layoutParams
        
        return CustomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val isLastVisible = isLastVisibleItem(position)
        val currentItem = if (isLastVisible && visibleItems.isNotEmpty()) {
            visibleItems[visibleItems.size - 1]
        } else {
            visibleItems.getOrNull(position)
        }
        holder.bind(currentItem, isLastVisible, position)
    }

    override fun getItemCount(): Int {
        return if (notVisibleItems.isNotEmpty()) {
            overlapLimit // Show all visible items including the count item
        } else {
            visibleItems.size.coerceAtMost(overlapLimit)
        }
    }

    /**
     * Check if the current position is the last visible item that shows count
     */
    private fun isLastVisibleItem(position: Int): Boolean {
        return position == overlapLimit - 1 && notVisibleItems.isNotEmpty()
    }

    inner class CustomViewHolder(val binding: RowImageBinding) : RecyclerView.ViewHolder(binding.root) {
        
        /**
         * Bind data to the view - shows either profile image or count
         */
        fun bind(overlapImageModel: OverlapImageModel?, isLastVisible: Boolean, position: Int) {
            if (isLastVisible) {
                // Show count on the last image
                val text = "+${notVisibleItems.size + 1}"
                val drawable = TextDrawable.builder()
                    .beginConfig()
                    .textColor(Color.WHITE)
                    .width(90)
                    .height(90)
                    .endConfig()
                    .buildRound(text, Color.parseColor("#2F88D6"))
                binding.imageView.setImageDrawable(drawable)
                
                // Set click listener for numbered item
                binding.root.setOnClickListener {
                    clickListener?.onNumberedItemClick(position)
                }
            } else {
                // Show profile image
                if (overlapImageModel?.imageUrl.isNullOrBlank()) {
                    Glide.with(binding.imageView.context)
                        .load(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .into(binding.imageView)
                } else {
                    Glide.with(binding.imageView.context)
                        .load(overlapImageModel?.imageUrl)
                        .apply(RequestOptions.circleCropTransform().priority(Priority.HIGH))
                        .error(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .placeholder(ContextCompat.getDrawable(context, R.drawable.profile_photo))
                        .into(binding.imageView)
                }
                
                // Set click listener for normal item
                binding.root.setOnClickListener {
                    clickListener?.onNormalItemClicked(position)
                }
            }
        }
    }
}l
