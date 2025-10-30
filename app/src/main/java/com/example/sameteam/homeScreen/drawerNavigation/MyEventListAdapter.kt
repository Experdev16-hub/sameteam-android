package com.example.sameteam.homeScreen.drawerNavigation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.databinding.EventCardLayoutBinding
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.EventModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter.OverlapAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.OverlapImageModel
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

// ADD THESE EXTENSION FUNCTIONS AT THE TOP OF THE FILE (after imports, before class)
import android.animation.ValueAnimator

// Extension function for addAnimation
fun View.addAnimation() {
    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            alpha = value
            scaleX = 0.8f + 0.2f * value
            scaleY = 0.8f + 0.2f * value
        }
    }
    animator.start()
}

// Extension function for adding multiple items
fun <T> MutableCollection<T>.addAll(vararg elements: T): Boolean {
    return addAll(elements.toList())
}

class MyEventListAdapter(
    val context: Context,
    val items: ArrayList<EventModel>,
    val onItemClicked: (Int, Int) -> Unit
) :
    RecyclerView.Adapter<MyEventListAdapter.MyViewHolder>() {

    //------limit number of items to be overlapped
    private val overlapLimit = 10

    //------set value of item overlapping
    private val overlapWidth = 30

    private val imageArrayList: ArrayList<OverlapImageModel> = ArrayList()

    class MyViewHolder(val binding: EventCardLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.event_card_layout,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]

        holder.binding.eventName.text = item.title
        holder.binding.eventDesc.text = item.event_description
        holder.binding.startDate.text = item.start_date
        holder.binding.endDate.text = item.end_date
        if (item.colour != null) {
            holder.binding.eventColor.imageTintList =
                ColorStateList.valueOf(Color.parseColor(item.colour))
        }

        holder.binding.imgEdit.visibility = if (item.editable == true) View.VISIBLE else View.GONE

        holder.binding.imgEdit.setOnClickListener {
            item.id?.let { it1 -> onItemClicked.invoke(it1,R.id.img_edit) }
        }

        holder.binding.ivCreateActivity.setOnClickListener {
            item.id?.let { it1 -> onItemClicked.invoke(it1,R.id.ivCreateActivity) }
        }

        val textArray = item.title?.trim()?.split(" ")?.toTypedArray()
        Log.i("check Lelength","ABCD")
        Log.i("check Lelength", Gson().toJson(textArray))
        if (textArray?.size!! < 2) {
            if (textArray[0].length < 2) holder.binding.initials.text =
                textArray[0][0].toString().uppercase(Locale.getDefault())
            else holder.binding.initials.text = textArray[0].substring(0, 2)
                .uppercase(Locale.getDefault())
        } else {
            holder.binding.initials.text =
                "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
        }

        /**
         * Adapter for loading stack(overlapping) images of users
         */
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        holder.binding.recView.layoutManager = layoutManager

        val adapter = OverlapAdapter(overlapLimit, overlapWidth)

        if (holder.binding.recView.itemDecorationCount < 1)
            holder.binding.recView.addItemDecoration(adapter.getItemDecoration())

        holder.binding.recView.adapter = adapter
        
        // FIX: Replace the problematic line with the extension function
        holder.itemView.addAnimation() // This calls the extension function we added
        
//        adapter.animationType = OverlapRecyclerViewAnimation.RIGHT_LEFT

        imageArrayList.clear()
        if (!item.event_user.isNullOrEmpty()) {
            for (participant in item.event_user) {
                if (participant.user?.profile_picture.isNullOrBlank()) imageArrayList.add(
                    OverlapImageModel("")
                )
                else imageArrayList.add(OverlapImageModel(participant.user?.profile_picture))
            }
        }

        // FIX: Use the extension function we added
        adapter.updateData(imageArrayList) // Use updateData instead of addAll
    }
}
