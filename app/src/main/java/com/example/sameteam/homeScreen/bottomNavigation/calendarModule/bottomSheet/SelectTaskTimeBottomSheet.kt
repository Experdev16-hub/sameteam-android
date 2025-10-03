package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetTaskTimeBinding
import com.example.sameteam.helper.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Calendar

class SelectTaskTimeBottomSheet(val location: String) : BottomSheetDialogFragment() {

    //Implemented in CreateTaskActivity
    interface TaskTimeListener {
        fun onTaskTimeDone(value: String, location: String)
    }

    lateinit var taskTimeListener: TaskTimeListener

    lateinit var binding: BottomSheetTaskTimeBinding
    val c = Calendar.getInstance()
    var hours = 1
    var minutes = 0
    var ampm = "PM"
    var firstTimeHour = true
    var firstTimeMinutes = true
    var firstTimeAMPM = true
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_task_time, container, false)

        /**
         * This will expand the bottom sheet up to maximum height needed for bottom sheet,
         * if height is more then scroll will be there
         */
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheet: View? =
                d.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val sheetBehavior = BottomSheetBehavior.from(bottomSheet!!)
            sheetBehavior.peekHeight = bottomSheet.height
        }

        if (location == Constants.START_DATE)
            binding.txtDate.text = "Start Time"
        else
            binding.txtDate.text = "End Time"

// NumberPicker doesn't support custom typeface directly
binding.numberPicker1.minValue = 1
binding.numberPicker1.maxValue = 12
binding.numberPicker1.value = 1  // Default to 1

binding.numberPicker2.minValue = 0
binding.numberPicker2.maxValue = 59
binding.numberPicker2.value = 0  // Default to 0

binding.numberPicker3.minValue = 0
binding.numberPicker3.maxValue = 1
binding.numberPicker3.displayedValues = arrayOf("AM", "PM")
binding.numberPicker3.value = 1  // Default to PM

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {

            taskTimeListener = activity as TaskTimeListener
            var mHour = ""
            var mMinutes = ""
            mHour = if (hours in 1..9) "0$hours" else "$hours"
            mMinutes = if (minutes in 0..9) "0$minutes" else "$minutes"
            taskTimeListener.onTaskTimeDone("$mHour:$mMinutes $ampm", location)
            dismiss()

        }

        /**
         * Hours wheel picker
         */


// CHANGE TO:
binding.numberPicker1.setOnValueChangedListener { picker, oldVal, newVal ->
    if (firstTimeHour) {
        moveToCurrentHour()
    }
    hours = newVal
}

        /**
         * Minutes wheel picker
         */


// C
HANGE TO:
binding.numberPicker2.setOnValueChangedListener { picker, oldVal, newVal ->
    if (firstTimeMinutes) {
        moveToCurrentMinute()
    }
    minutes = newVal
}

        /**
         * AM_PM wheel picker
         */

binding.numberPicker3.setOnValueChangedListener { picker, oldVal, newVal ->
    if (firstTimeAMPM) {
        moveToCurrentAMPM()
    }
    ampm = if (newVal == 0) "AM" else "PM"
}

    }

 private fun moveToCurrentAMPM() {
    val minute = c.get(Calendar.MINUTE)
    Log.i("Minutes", c.get(Calendar.MINUTE).toString())
    Log.i("AMPM", c.get(Calendar.AM_PM).toString()) // shows 0 when AM

    if (c.get(Calendar.AM_PM) == 0) {
        ampm = "AM"
        binding.numberPicker3.value = 0
        firstTimeAMPM = false
    } else {
        ampm = "PM"
        binding.numberPicker3.value = 1
        firstTimeAMPM = false
    }
}
private fun moveToCurrentMinute() {
    val minute = c.get(Calendar.MINUTE)
    Log.i("Minutes", c.get(Calendar.MINUTE).toString())

    minutes = minute
    binding.numberPicker2.value = minute
    firstTimeMinutes = false
}

private fun moveToCurrentHour() {
    val hour = c.get(Calendar.HOUR)
    Log.i("Hour", c.get(Calendar.HOUR).toString())

    hours = hour
    binding.numberPicker1.value = hour
    firstTimeHour = false
}



}
