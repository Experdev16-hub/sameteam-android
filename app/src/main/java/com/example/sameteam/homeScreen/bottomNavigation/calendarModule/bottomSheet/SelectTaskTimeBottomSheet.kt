package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
//import com.aigestudio.wheelpicker.WheelPicker
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


        val selectedTF = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_demibold)
        //binding.wheelPicker.typeface = selectedTF
        //binding.wheelPicker2.typeface = selectedTF
       // binding.wheelPicker3.typeface = selectedTF
       // binding.wheelPicker.data = (1..12).toList()

        val temp = arrayListOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09")
        (10..59).forEach { temp.add(it.toString()) }
        //binding.wheelPicker2.data = temp
       // binding.wheelPicker3.data = listOf("AM", "PM")
        return binding.root
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
      /*  binding.wheelPicker.setOnWheelChangeListener(object : WheelPicker.OnWheelChangeListener {
            override fun onWheelScrolled(offset: Int) {
                if (firstTimeHour) {
                    moveToCurrentHour()
                }
            }

            override fun onWheelSelected(position: Int) {
                hours = position + 1
            }

            override fun onWheelScrollStateChanged(state: Int) {
            }

        })

        /**
         * Minutes wheel picker
         */
       binding.wheelPicker2.setOnWheelChangeListener(object : WheelPicker.OnWheelChangeListener {
            override fun onWheelScrolled(offset: Int) {
                if (firstTimeMinutes) {
                    moveToCurrentMinute()
                }
            }


            override fun onWheelSelected(position: Int) {
                minutes = position
            }

            override fun onWheelScrollStateChanged(state: Int) {
            }
        })

        /**
         * AM_PM wheel picker
         */
          binding.wheelPicker3.setOnWheelChangeListener(object : WheelPicker.OnWheelChangeListener {
            override fun onWheelScrolled(offset: Int) {
                if (firstTimeAMPM)
                    moveToCurrentAMPM()

            }

            override fun onWheelSelected(position: Int) {
                ampm = if (position == 0)
                    "AM"
                else
                    "PM"
            }

            override fun onWheelScrollStateChanged(state: Int) {

            }
        })*/

    }

    private fun moveToCurrentAMPM() {
        val minute = c.get(Calendar.MINUTE)
        Log.i("Minutes", c.get(Calendar.MINUTE).toString())
        Log.i("AMPM", c.get(Calendar.AM_PM).toString()) // shows 0 when AM
        Log.i("AM", c.get(Calendar.AM).toString()) // shows 1 when AM
        Log.i("PM", c.get(Calendar.PM).toString()) // shows 2023 when AM


        if (c.get(Calendar.AM_PM) == 0) {
            ampm = "AM"
            
            firstTimeAMPM = false
        } else {
            ampm = "PM"
            
            firstTimeAMPM = false
        }
    }

    private fun moveToCurrentMinute() {
        val minute = c.get(Calendar.MINUTE)
        Log.i("Minutes", c.get(Calendar.MINUTE).toString())
        //Log.i("AMPM", c.get(Calendar.AM_PM).toString()) // shows 1 when PM both .AM and .AMPM
        for (i in 0..59) {
            Log.i("Time for", i.toString())
            if (i == minute) {
                minutes = minute
                
                firstTimeMinutes = false
                break
            }
        }
    }

    private fun moveToCurrentHour() {
        val hour = c.get(Calendar.HOUR)
        Log.i("Hour", c.get(Calendar.HOUR).toString())

        for (i in 1..12) {
            Log.i("Time for", i.toString())
            if (i == hour) {
                hours = hour
                
                firstTimeHour = false
                break
            }
        }
    }

}
