package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.databinding.TaskCardLayoutBinding
import com.example.sameteam.helper.*
import com.example.sameteam.homeScreen.HomeActivity
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.model.TaskDetailsResponseModel
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.bottomSheet.TaskDetailsBottomSheet
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.interfaceses.OnBottomSheetDismissListener
import com.example.sameteam.widget.ConfirmDialog
import com.example.sameteam.widget.ConfirmDialog.ConfirmClickListener

class CalendarTaskAdapter(val listener: OnBottomSheetDismissListener,val context: Context) :
    PagingDataAdapter<TaskDetailsResponseModel.Data, CalendarTaskAdapter.MyViewHolder>(
        DataDifferentiator
    ),
    ConfirmClickListener {

    interface confirmTaskListener {
        fun confirmTask(place: String)
    }

    lateinit var varConfirmTaskListener: confirmTaskListener

    fun setConfirmTaskListener(varConfirmTaskListener: confirmTaskListener): Void? {

        this.varConfirmTaskListener = varConfirmTaskListener;
        return null;

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

    class MyViewHolder(val binding: TaskCardLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        val loggedUser = SharedPrefs.getUser(context)

        holder.binding.taskName.text = item?.name

        if (item?.event != null) {
            holder.binding.eventName.text = item.event?.title
            holder.binding.eventName.visibility = View.VISIBLE
        } else
            holder.binding.eventName.visibility = View.GONE

        if (item?.all_day == true) holder.binding.time.text = context.getString(R.string.all_day)
        else {
//            holder.binding.time.text = "${utcToLocal(item?.start_time.toString())} - ${utcToLocal(item?.end_time.toString())}"

            val startLocalTimestamp = utcTimestampToLocalDateTime(item?.start_time_stamp.toString())
            val endLocalTimestamp = utcTimestampToLocalDateTime(item?.end_time_stamp.toString())

            if (startLocalTimestamp != null && endLocalTimestamp != null) {
                val startDate = startLocalTimestamp.toLocalDate()?.let { getFormattedDate(it) }
                val endDate = endLocalTimestamp.toLocalDate()?.let { getFormattedDate(it) }

                if (startDate == endDate) {
//                    val timeString = startDate + ", ${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))} - ${getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))}"
                    val timeString =
                        "${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))} - ${
                            getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))
                        }"
                    holder.binding.time.text = timeString
                } else {
//                    val startTimeString = startDate + ", ${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))}"
//                    val endTimeString = endDate + ", ${getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))}"
                    val startTimeString =
                        "${getProperString(getFormattedTime(startLocalTimestamp.toLocalTime()))}"
                    val endTimeString =
                        "${getProperString(getFormattedTime(endLocalTimestamp.toLocalTime()))}"
                    holder.binding.time.text = "$startTimeString - $endTimeString"
                }
            }
        }

        if (item?.completed == true)
            setCompleteStatus(
                true,
                holder.binding.btnCompleted,
                holder.binding.imageView,
                holder.binding.detailLayout
            )
        else
            setCompleteStatus(
                false,
                holder.binding.btnCompleted,
                holder.binding.imageView,
                holder.binding.detailLayout
            )


        /**
         * Task Completed button click
         */
        holder.binding.btnCompleted.setOnClickListener {
            if (item?.user?.id == loggedUser?.id) {
                if (item?.completed == true) {
                    val confirmDialog =
                        ConfirmDialog(
                            context,
                            "This task is already completed",
                            "AlreadyCompleted"
                        )

//                    confirmDialog.show((context as HomeActivity).supportFragmentManager, "Confirm")
                    confirmDialog.setConfirmClickListener(this)
                    confirmDialog.show((context as HomeActivity).supportFragmentManager, "Confirm")
                } else {
                    val confirmDialog =
                        ConfirmDialog(
                            context,
                            "Are you sure you want to change the status of the task for all participants? This action cannot be undone.",
                            "Complete ${item?.id}"
                        )
                    confirmDialog.setConfirmClickListener(this)
                    confirmDialog.show((context as HomeActivity).supportFragmentManager, "Confirm")
                }
            } else {
//                Toast.makeText(context, "You cannot edit this task", Toast.LENGTH_LONG).show()
                Toast.makeText(
                    context,
                    context.getString(R.string.alert_tasks_created_by_others_cannot_be_edited),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        /**
         * When task item is clicked navigate to TaskDetailsBottomSheet
         */
        holder.binding.rootLayout.setOnClickListener {
            if (item?.id != null) {
                val fragment = TaskDetailsBottomSheet(listener,context, item.id)
                if (context is HomeActivity)
                    fragment.show(
                        context.supportFragmentManager,
                        TaskDetailsBottomSheet::class.java.name
                    )
                else{
                    Log.e("02/10 else -=-=->","")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.task_card_layout,
                parent,
                false
            )
        )
    }

    private fun setCompleteStatus(
        isComplete: Boolean,
        view: RelativeLayout,
        imageView: ImageView,
        view2: RelativeLayout
    ) {
        if (isComplete) {
            view.background.setTint(ContextCompat.getColor(context, R.color.green))
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.white))
            view2.visibility = View.GONE
        } else {
            view.background.setTint(ContextCompat.getColor(context, R.color.white))
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.black))
            view2.visibility = View.VISIBLE
        }
    }

    private fun getProperString(text: String): String {
        val array = text.split(":").toTypedArray()
        val hours = array[0].toInt()
        return "$hours:${array[1]}"
    }

    override fun onConfirm(place: String) {

//        Toast.makeText(context, "place -=-==--=> $place", Toast.LENGTH_SHORT).show()

        if (varConfirmTaskListener != null) {
//            snapshot().toMutableList().apply { removeAt(0) }
//            notifyItemRemoved(0)
            varConfirmTaskListener.confirmTask(place)

//            pageList?.dataSource?.invalidate()
//            notifyItemRemoved(1)
//            notifyDataSetChanged()
        }

    }

}