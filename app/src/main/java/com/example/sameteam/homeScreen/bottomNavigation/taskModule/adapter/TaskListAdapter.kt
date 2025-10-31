package com.example.sameteam.homeScreen.bottomNavigation.taskModule.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sameteam.R
import com.example.sameteam.databinding.TaskDetailsCardLayoutBinding
import com.example.sameteam.helper.getFormattedDate
import com.example.sameteam.helper.getFormattedTime
import com.example.sameteam.helper.utcTimestampToLocalDateTime
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.bottomSheet.ParticipantsBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.bottomSheet.TaskDetailsBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.OverlapImageModel
import com.example.sameteam.homeScreen.drawerNavigation.activity.MyEventsActivity
import com.mindinventory.overlaprecylcerview.listeners.OverlapRecyclerViewClickListener
import java.util.*
import kotlin.collections.ArrayList

class TaskListAdapter(val listener: OnBottomSheetDismissListener, val context: Context) :
    PagingDataAdapter<TaskDetailsResponseModel.Data, TaskListAdapter.MyViewHolder>(
        DataDifferentiator
    ), OverlapRecyclerViewClickListener {

    private val TAG = "TaskListAdapter"

    //------limit number of items to be overlapped
    private val overlapLimit = 8

    //------set value of item overlapping
    private val overlapWidth = 30
    private var acceptedCount = 0
    private var pendingCount = 0
    private var cancelCount = 0
    private val imageArrayList: ArrayList<OverlapImageModel> = ArrayList()

    class MyViewHolder(val binding: TaskDetailsCardLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.task_details_card_layout,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val item = getItem(position)

        holder.binding.taskName.text = item?.name

        holder.binding.textActivityCreator.text = item?.firstName + " " + item?.lastName
        if (!item?.description.isNullOrBlank()) {
            holder.binding.taskDesc.visibility = View.VISIBLE
            holder.binding.taskDesc.text = item?.description
        } else
            holder.binding.taskDesc.visibility = View.GONE

        if (!item?.location.isNullOrBlank()) {
            holder.binding.location.text = item?.location
            holder.binding.locationLayout.visibility = View.VISIBLE
        } else
            holder.binding.locationLayout.visibility = View.GONE

        if (item?.event != null) {
            holder.binding.eventName.text = item.event?.title
            holder.binding.eventName.visibility = View.VISIBLE
        } else
            holder.binding.eventName.visibility = View.GONE

        if (item?.team_name != null) {
            holder.binding.teamNames.text = "Teams: ${item.team_name}"
            holder.binding.teamNames.visibility = View.VISIBLE
        } else
            holder.binding.teamNames.visibility = View.GONE


        if (item?.total_slots == 0 || item?.total_slots == null) {
            holder.binding.slotLayout.visibility = View.GONE
        } else {
            holder.binding.slotLayout.visibility = View.VISIBLE
            holder.binding.txtSlots.text =
                "${item?.available_slots} of ${item?.total_slots} Slots Available"
        }

        try {
            if (item != null) {
                if (item.event?.colour != null) {
                    holder.binding.eventColor.imageTintList = ColorStateList.valueOf(
                        Color.parseColor(
                            item.event?.colour
                        )
                    )
                } else {
                    holder.binding.eventColor.imageTintList =
                        ColorStateList.valueOf(Color.parseColor("#ffffff"))
                }
            } else {
                holder.binding.eventColor.imageTintList =
                    ColorStateList.valueOf(Color.parseColor("#ffffff"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "onBindViewHolder: Item color ${item?.event?.colour}")
            holder.binding.eventColor.imageTintList =
                ColorStateList.valueOf(Color.parseColor("#ffffff"))
        }


        val startLocalTimestamp = utcTimestampToLocalDateTime(item?.start_time_stamp.toString())
        val startDate = startLocalTimestamp?.toLocalDate()?.let { getFormattedDate(it) }

        holder.binding.start.text = startDate


        if (item?.all_day == true) {
            holder.binding.endTime.text = context.getString(R.string.all_day)
        } else {
            val endLocalTimestamp = utcTimestampToLocalDateTime(item?.end_time_stamp.toString())

            if (startLocalTimestamp != null && endLocalTimestamp != null) {

                val startTimeString =
                    getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))
                val endTimeString =
                    getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))

                holder.binding.endTime.text = "$startTimeString - $endTimeString"
            }
        }

        /**
         * If task has image url, show image
         */
        if (item?.image_url != null && item.image_url != "") {
            holder.binding.taskImage.visibility = View.VISIBLE
            holder.binding.initials.visibility = View.GONE

            Glide.with(context)
                .load(item.image_url)
                .error(R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .circleCrop()
                .into(holder.binding.taskImage)
        } else {
            /**
             * If task does not has image url, show task name initials
             */

            holder.binding.taskImage.visibility = View.GONE
            holder.binding.initials.visibility = View.VISIBLE

            val textArray = item?.name?.split(" ")?.filter { it.isNotBlank() }?.toTypedArray()
            if (textArray?.size!! < 2) {
                if (textArray[0].length < 2) holder.binding.initials.text =
                    textArray[0][0].toString().uppercase(Locale.getDefault())
                else holder.binding.initials.text = textArray[0].substring(0, 2)
                    .uppercase(Locale.getDefault())
            } else {
                holder.binding.initials.text =
                    "${textArray[0][0].uppercaseChar()}${textArray[1][0].uppercaseChar()}"
            }
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

        imageArrayList.clear()


        if (item?.task_participants?.isNotEmpty() == true) {
            acceptedCount = 0
            cancelCount = 0
            pendingCount = 0
            holder.binding.participantsLayout.visibility =
                View.GONE //Visibility set to Gone as needed to hide layout
            for (participant in item.task_participants) {
                if (participant.response == "accepted") {
                    acceptedCount++
                } else if (participant.response == "declined") {
                    cancelCount++

                } else if (participant.response == "pending") {
                    pendingCount++
                }
                if (participant.user?.profile_picture.isNullOrBlank()) imageArrayList.add(
                    OverlapImageModel("")
                )
                else imageArrayList.add(OverlapImageModel(participant.user?.profile_picture))
            }
            holder.binding.txtAcceptedCount.text = acceptedCount.toString()
            holder.binding.txtCancelCount.text = cancelCount.toString()
            holder.binding.txtPendingCount.text = pendingCount.toString()
        } else {
            holder.binding.participantsLayout.visibility = View.GONE
            holder.binding.participantsStatusLayout.visibility = View.GONE

        }

        // Use updateData instead of addAll
        adapter.updateData(imageArrayList)
        
        // Use setOverlapRecyclerViewClickListener instead of direct property assignment
        adapter.setOverlapRecyclerViewClickListener(this)

        /**
         * When task item is clicked, it shows TaskDetailsBottomSheet
         */
        holder.binding.parent.setOnClickListener {

            val fragment = TaskDetailsBottomSheet(listener, context, (item?.id as? Int) ?: 0)
            if (context is HomeActivity)
                fragment.show(
                    context.supportFragmentManager,
                    TaskDetailsBottomSheet::class.java.name
                )
            else if (context is MyEventsActivity)
                fragment.show(
                    context.supportFragmentManager,
                    TaskDetailsBottomSheet::class.java.name
                )
        }

        holder.binding.participantsLayout.setOnClickListener {
            item?.let { it1 -> participantsLayoutClicked(it1) }
        }
        holder.binding.clickLayout.setOnClickListener {
            item?.let { it1 -> participantsLayoutClicked(it1) }
        }
        holder.binding.participantsStatusLayout.setOnClickListener {
            item?.let { it1 -> participantsLayoutClicked(it1) }
        }
    }

    private fun participantsLayoutClicked(item: TaskDetailsResponseModel.Data) {
        val fragment = ParticipantsBottomSheet(context, item.task_participants)
        if (context is HomeActivity)
            fragment.show(
                context.supportFragmentManager,
                ParticipantsBottomSheet::class.java.name
            )
        else if (context is MyEventsActivity)
            fragment.show(
                context.supportFragmentManager,
                ParticipantsBottomSheet::class.java.name
            )
    }

    override fun onNormalItemClicked(adapterPosition: Int) {
        // Implementation for normal item click
    }

    override fun onNumberedItemClick(adapterPosition: Int) {
        // Implementation for numbered item click
    }

    object DataDifferentiator : DiffUtil.ItemCallback<TaskDetailsResponseModel.Data>() {
        override fun areItemsTheSame(
            oldItem: TaskDetailsResponseModel.Data,
            newItem: TaskDetailsResponseModel.Data
        ): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: TaskDetailsResponseModel.Data,
            newItem: TaskDetailsResponseModel.Data
        ): Boolean {
            return oldItem == newItem
        }
    }

    private fun getProperString(text: String): String {
        val array = text.split(":").toTypedArray()
        val hours = array[0].toInt()
        return "$hours:${array[1]}"
    }
    }
