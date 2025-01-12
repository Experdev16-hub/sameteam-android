package com.example.sameteam.homeScreen.bottomNavigation.notificationModule.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.TimeUtils
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.databinding.*
import com.example.sameteam.helper.*
import com.example.sameteam.helper.Constants.NOTIFICATION_VIEW_TYPE_1
import com.example.sameteam.helper.Constants.NOTIFICATION_VIEW_TYPE_2
import com.example.sameteam.helper.Constants.NOTIFICATION_VIEW_TYPE_3
import com.example.sameteam.helper.Constants.NOTIFICATION_VIEW_TYPE_4
import com.example.sameteam.helper.Constants.NOTIFICATION_VIEW_TYPE_5
import com.example.sameteam.homeScreen.bottomNavigation.notificationModule.model.NotificationModel
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NotificationAdapter(val context: Context, val items:ArrayList<NotificationModel>, var onItemClick: ((NotificationModel, Int, Int) -> Unit)? = null)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    class ViewHolder1(val binding1: NotificationCardLayout1Binding) : RecyclerView.ViewHolder(binding1.root)
    class ViewHolder2(val binding2: NotificationCardLayout2Binding) : RecyclerView.ViewHolder(binding2.root)
    class ViewHolder3(val binding3: NotificationCardLayout3Binding) : RecyclerView.ViewHolder(binding3.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            NOTIFICATION_VIEW_TYPE_1, NOTIFICATION_VIEW_TYPE_5 -> {
                ViewHolder1(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_card_layout_1,parent,false))
            }
            NOTIFICATION_VIEW_TYPE_2 -> {
                ViewHolder2(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_card_layout_2,parent,false))
            }
            NOTIFICATION_VIEW_TYPE_3, NOTIFICATION_VIEW_TYPE_4 -> {
                ViewHolder3(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_card_layout_3,parent,false))
            }
            else -> {
                ViewHolder1(DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.notification_card_layout_1,parent,false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item = items[position]

        when(getItemViewType(position)){
            NOTIFICATION_VIEW_TYPE_1, NOTIFICATION_VIEW_TYPE_5 -> {
                onBindMessageViewHolder(holder as ViewHolder1, item)
            }
            NOTIFICATION_VIEW_TYPE_2 -> {
                onBindInvitationViewHolder(holder as ViewHolder2, item)
            }
            NOTIFICATION_VIEW_TYPE_3,NOTIFICATION_VIEW_TYPE_4 -> {
                onBindResponseViewHolder(holder as ViewHolder3, item)
            }
            else -> {
                onBindMessageViewHolder(holder as ViewHolder1, item)
            }
        }

    }

    private fun onBindMessageViewHolder(holder: ViewHolder1, item : NotificationModel){

//        if(item.time != null){
//            holder.binding1.time.visibility = View.VISIBLE
//            holder.binding1.time.text =  getProperString(utcToLocal(item.time!!))
//        }
//        else  holder.binding1.time.visibility = View.GONE

        if(item.notification_time != null){
            holder.binding1.time.visibility = View.VISIBLE

            val localTimestamp = utcTimestampToLocalDateTime(item.notification_time.toString())
            if(localTimestamp != null) {
                val timeString = getProperString(getFormattedTime(localTimestamp.toLocalTime()))
                holder.binding1.time.text =  timeString

            }
            else{
                holder.binding1.time.visibility = View.GONE
            }

        }
        else  holder.binding1.time.visibility = View.GONE


        holder.binding1.message.text = item.description

    }

    private fun onBindInvitationViewHolder(holder: ViewHolder2, item : NotificationModel){
        holder.binding2.taskName.text = item.description
//        if(item.not != null){
//            holder.binding2.time.visibility = View.VISIBLE
//            holder.binding2.time.text =  getProperString(utcToLocal(item.time!!))
//        }
//        else  holder.binding2.time.visibility = View.GONE

        if(item.notification_time != null){
            holder.binding2.time.visibility = View.VISIBLE

            val localTimestamp = utcTimestampToLocalDateTime(item.notification_time.toString())
            if(localTimestamp != null) {
                val timeString = getProperString(getFormattedTime(localTimestamp.toLocalTime()))
                holder.binding2.time.text =  timeString

            }
            else{
                holder.binding2.time.visibility = View.GONE
            }

        }
        else  holder.binding2.time.visibility = View.GONE

        val localStartTimeStamp = utcTimestampToLocalDateTime(item.task?.start_time_stamp.toString())
        val localEndTimeStamp = utcTimestampToLocalDateTime(item.task?.end_time_stamp.toString())

        if(item.task?.all_day == true){
            holder.binding2.taskTime.text = context.getString(R.string.all_day)
            val localFormattedDate = localStartTimeStamp?.let { getFormattedDate(it.toLocalDate()) }
            holder.binding2.taskDate.text = localFormattedDate
            holder.binding2.taskDate.visibility = View.VISIBLE
        }
        else{

            if(localStartTimeStamp != null && localEndTimeStamp != null){
                holder.binding2.taskDate.visibility = View.VISIBLE

                val localFormattedDate = getFormattedDate(localStartTimeStamp.toLocalDate())
                holder.binding2.taskDate.text = localFormattedDate

                val startString = getProperString(getFormattedTime(localStartTimeStamp.toLocalTime()))
                val endString = getProperString(getFormattedTime(localEndTimeStamp.toLocalTime()))
                holder.binding2.taskTime.text = "$startString - $endString"
            }
            else{
                val startString = getProperString(utcToLocal(item.task?.start_time!!))
                val endString = getProperString(utcToLocal(item.task?.end_time!!))
                holder.binding2.taskTime.text = "$startString - $endString"
                holder.binding2.taskDate.visibility = View.GONE
            }
        }

//        if(item.date != null){
//            val splitArray = item.date?.split("-")
//            if(!splitArray?.get(1).isNullOrBlank() || !splitArray?.get(0).isNullOrBlank() ) {
//               holder.binding2.monthName.text = splitArray?.get(1)
//               holder.binding2.date.text = splitArray?.get(0)
//                holder.binding2.dateLayout.visibility = View.VISIBLE
//            }
//            else
//                holder.binding2.dateLayout.visibility = View.INVISIBLE
//        }
//        else
//            holder.binding2.dateLayout.visibility = View.INVISIBLE


        if(item.participant_response != null ) {
            if(item.participant_response == Constants.PENDING){
                holder.binding2.accept.text = context.getString(R.string.accept)
                holder.binding2.decline.text = context.getString(R.string.decline)
                setUnselected(holder.binding2.accept)
                setUnselected(holder.binding2.decline)
                setImage(holder, Constants.PENDING)
            }
            if(item.participant_response == Constants.ACCEPTED) {
                holder.binding2.accept.text = "Accepted"
                holder.binding2.decline.text = context.getString(R.string.decline)
                setSelected(holder.binding2.accept)
                setUnselected(holder.binding2.decline)
                setImage(holder, Constants.ACCEPTED)
            }
            if(item.participant_response == Constants.DECLINED){
                holder.binding2.accept.text = context.getString(R.string.accept)
                holder.binding2.decline.text = "Declined"
                setUnselected(holder.binding2.accept)
                setSelected(holder.binding2.decline)
                setImage(holder, Constants.DECLINED)
            }
        }
        else {
            setUnselected(holder.binding2.accept)
            setUnselected(holder.binding2.decline)
            holder.binding2.thumbImage.visibility = View.GONE
            holder.binding2.bellLayout.visibility = View.VISIBLE
        }

        holder.binding2.accept.setOnClickListener {
            if(item.task_participant_id != null ){
                if(item.participant_response == Constants.ACCEPTED)
                    Toast.makeText(context, "Response already recorded", Toast.LENGTH_SHORT).show()
                else
                    onItemClick?.invoke(item, holder.absoluteAdapterPosition,0)
            }
        }

        holder.binding2.decline.setOnClickListener {
            if(item.task_participant_id != null ){
                if(item.participant_response == Constants.DECLINED)
                    Toast.makeText(context, "Response already recorded", Toast.LENGTH_SHORT).show()
                else
                    onItemClick?.invoke(item, holder.absoluteAdapterPosition,1)
            }
        }
    }

    private fun onBindResponseViewHolder(holder: ViewHolder3, item : NotificationModel){
        holder.binding3.message.text = item.description
        if(item.time != null){
            holder.binding3.time.visibility = View.VISIBLE
            holder.binding3.time.text =  getProperString(utcToLocal(item.time!!))
        }
        else  holder.binding3.time.visibility = View.GONE

        if(item.notification_type == "3"){
            holder.binding3.thumbImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_notification_check))
        }
        else if(item.notification_type == "4"){
            holder.binding3.thumbImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_notification_cancel))
        }
    }

    private fun setImage(holder: ViewHolder2, response: String){

        if(response == Constants.PENDING){
            holder.binding2.thumbImage.visibility = View.GONE
            holder.binding2.bellLayout.visibility = View.VISIBLE
        }
        else if(response == Constants.ACCEPTED){
            holder.binding2.bellLayout.visibility = View.GONE

            holder.binding2.thumbImage.visibility = View.VISIBLE
            holder.binding2.thumbImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_notification_check))
        }
        else if(response == Constants.DECLINED){
            holder.binding2.bellLayout.visibility = View.GONE

            holder.binding2.thumbImage.visibility = View.VISIBLE
            holder.binding2.thumbImage.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_notification_cancel))
        }
    }


    private fun setDate(date: String): String{

        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val newDate = sdf.parse(date)
        val millis = newDate?.time
        val notificationTime = Calendar.getInstance()

        var title = ""
        if (millis != null) {
            notificationTime.timeInMillis = millis


            val now = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)
            val lastYearFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)

            val sameDay = now.get(Calendar.DATE) == notificationTime.get(Calendar.DATE)
            val sameMonth = now.get(Calendar.MONTH) == notificationTime.get(Calendar.MONTH)
            val lastDay = now.get(Calendar.DAY_OF_YEAR) - notificationTime.get(Calendar.DAY_OF_YEAR) == 1
            val sameYear = now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR)

            if (sameDay && sameYear && sameMonth) {
                title = context.getString(R.string.today)
            } else if (lastDay && sameYear) {
                title = context.getString(R.string.yesterday)
            } else if (sameYear) {
                title = dateFormat.format(Date(millis))
            } else {
                title = lastYearFormat.format(Date(millis))
            }
        }

       return title
    }

    private fun setSelected(textview: TextView){

        textview.setBackgroundResource(R.drawable.tab_selected_background)
        textview.setTextColor(ContextCompat.getColor(context, R.color.white))
        textview.setTypeface(textview.typeface,Typeface.BOLD)
    }

    private fun setUnselected(textview: TextView){
        textview.setBackgroundResource(R.color.white)
        textview.setTextColor(ContextCompat.getColor(context, R.color.black))
        textview.setTypeface(null, Typeface.NORMAL)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].notification_type?.toInt()!!
    }

    private fun getProperString(text: String): String{
        val array = text.split(":").toTypedArray()
        val hours = array[0].toInt()
        return "$hours:${array[1]}"
    }

    override fun getHeaderId(position: Int): Long {
        val item = items[position]
        return run {
            val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val newDate = sdf.parse(item.date!!)
            val millis = newDate?.time
            if (millis != null) {
                getDateAsHeaderId(millis)
            } else{
                0L
            }
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent?.context)
            .inflate(R.layout.chat_message_header, parent, false)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindHeaderViewHolder(p0: RecyclerView.ViewHolder?, p1: Int) {
        val view = p0?.itemView
        val dateTextView = view?.findViewById<TextView>(R.id.txtHeader)

        val item = items[p1]

        if(dateTextView != null && item.date != null){
            dateTextView.text = setDate(item.date!!)
        }

        val layoutParams = dateTextView?.layoutParams as LinearLayout.LayoutParams
        layoutParams.topMargin = 0
        dateTextView.layoutParams = layoutParams
    }
}