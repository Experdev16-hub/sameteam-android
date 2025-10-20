package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
//import com.aigestudio.wheelpicker.WheelPicker
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetCustomRepeatTaskBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.homeScreen.bottomNavigation.calendarModule.activity.CreateTaskActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kizitonwose.calendarview.utils.yearMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList


class CustomRepeatTaskBottomSheet(val localDate: LocalDate) : BottomSheetDialogFragment(),
    View.OnClickListener, WheelPicker.OnWheelChangeListener {

    //Implemented in CreateTaskActivity
    interface CustomListener{
        fun onCustomSelected(list: ArrayList<String?>, dateString: String? , times: String?)
    }

    lateinit var customListener: CustomListener

    private val TAG = "CustomRepeatTaskBottomS"

    lateinit var binding: BottomSheetCustomRepeatTaskBinding
    var wheelPosition = 0

    var endStatus: String? = "Never"
    var mSpecificDate: String? = null
    var mTimes: String? = null
    var arrayList = ArrayList<String?>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.bottom_sheet_custom_repeat_task,
            container,
            false
        )

        binding.monthRepeatDate.text = localDate.dayOfMonth.toString()  //Set the date from the date selected in create task screen
        binding.yearRepeatDate.text = "${localDate.dayOfMonth}-${ 
            localDate.yearMonth.month.getDisplayName(
                TextStyle.SHORT,
                Locale.ENGLISH
            )
        }"   //Set the Date-Month from the date selected in create task screen


        //By default keep the weekday selected from the date selected in create task screen
        when (localDate.dayOfWeek.value) {
            1 -> {
                binding.mon.tag = "1"
                setWeekSelected(binding.mon)
            }
            2 -> {
                binding.tue.tag = "1"
                setWeekSelected(binding.tue)
            }
            3 -> {
                binding.wed.tag = "1"
                setWeekSelected(binding.wed)
            }
            4 -> {
                binding.thu.tag = "1"
                setWeekSelected(binding.thu)
            }
            5 -> {
                binding.fri.tag = "1"
                setWeekSelected(binding.fri)
            }
            6 -> {
                binding.sat.tag = "1"
                setWeekSelected(binding.sat)
            }
            7 ->{
                binding.sun.tag = "1"
                setWeekSelected(binding.sun)
            }
        }


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


        val selectedTF = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_demibold)
        //binding.wheelPicker.typeface = selectedTF
        //binding.wheelPicker.data = listOf("Day", "Week", "Month", "Year")
        //binding.wheelPicker.setOnWheelChangeListener(this)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnTimes.setOnClickListener {
            val fragment = RepeatTimesBottomSheet()
            fragment.show(
                requireActivity().supportFragmentManager,
                RepeatTimesBottomSheet::class.java.name
            )
        }

        binding.btnSpecificDate.setOnClickListener {
            val fragment = SpecificDateBottomSheet(Constants.SPECIFIC_DATE)
            fragment.show(
                requireActivity().supportFragmentManager,
                SpecificDateBottomSheet::class.java.name
            )
        }

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnNever.setOnClickListener {
            endStatus = context?.getString(R.string.never)
            setSelected(binding.btnNever, binding.txtNever, binding.check)
            setUnselected(binding.btnSpecificDate, binding.txtSpecificDate, binding.specificDate)
            setUnselected(binding.btnTimes, binding.txtTimes, binding.times)
        }

        binding.mon.setOnClickListener(this)
        binding.tue.setOnClickListener(this)
        binding.wed.setOnClickListener(this)
        binding.thu.setOnClickListener(this)
        binding.fri.setOnClickListener(this)
        binding.sat.setOnClickListener(this)
        binding.sun.setOnClickListener(this)


        binding.btnDone.setOnClickListener {
            arrayList.clear()
            when(wheelPosition){
                0 -> {
                    //If Daily option is selected arraylist will only have "Daily"
                    arrayList.add(context?.getString(R.string.daily))
                }
                1 -> {
                    if(weekValidation()){
                        //If Week option is selected arraylist will have all selected weeks
                        getWeekDays()
                    }
                }
                2 -> {
                    arrayList.add(context?.getString(R.string.monthly))
                }
                3 -> {
                    arrayList.add(context?.getString(R.string.yearly))
                }
            }

            if(!arrayList.isNullOrEmpty()){
                if(endStatus == context?.getString(R.string.never)){
                    customListener = context as CustomListener
                    customListener.onCustomSelected(arrayList, null, null)
                }
                else if(endStatus == context?.getString(R.string.times) && !mTimes.isNullOrBlank()){
                    customListener = context as CustomListener
                    customListener.onCustomSelected(arrayList, null, mTimes)
                }
                else if(endStatus == context?.getString(R.string.specific_date) && !mSpecificDate.isNullOrBlank()){
                    customListener = context as CustomListener
                    customListener.onCustomSelected(arrayList, mSpecificDate, null)
                }
                dismiss()
            }

        }


        /**
         * The number of times task will be repeated is observe by this, observeTimes is in CreateTaskVM
         */
        (activity as CreateTaskActivity).createTaskVM.observeTimes().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (it.isNotBlank()) {

                    endStatus = context?.getString(R.string.times)
                    mTimes = it
                    binding.times.text = it

                    setSelected(binding.btnTimes, binding.txtTimes, binding.times)
                    setUnselected(binding.btnNever, binding.txtNever, binding.check)
                    setUnselected(
                        binding.btnSpecificDate,
                        binding.txtSpecificDate,
                        binding.specificDate
                    )

                }
            }
        }

        /**
         * The end date until the task will be repeated is observe by this, observeSpecificDate is in CreateTaskVM
         */
        (activity as CreateTaskActivity).createTaskVM.observeSpecificDate().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (it.isNotBlank()) {

                    endStatus = context?.getString(R.string.specific_date)
                    mSpecificDate = it
                    binding.specificDate.text = it

                    setUnselected(binding.btnTimes, binding.txtTimes, binding.times)
                    setUnselected(binding.btnNever, binding.txtNever, binding.check)
                    setSelected(
                        binding.btnSpecificDate,
                        binding.txtSpecificDate,
                        binding.specificDate
                    )

                }
            }
        }

    }

    /**
     * This will set the unselected view style for "Ends" options
     */
    fun setUnselected(background: RelativeLayout, txt: TextView, value: View?) {
        background.setBackgroundResource(R.color.white)
        txt.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_regular)
        txt.setTextColor(ContextCompat.getColor(requireContext(), R.color.darkGrey))
        if (value != null) {
            value.visibility = View.INVISIBLE
        }
    }

    /**
     * This will set the selected view style for "Ends" options
     */
    private fun setSelected(background: RelativeLayout, txt: TextView, value: View?) {
        background.setBackgroundResource(R.color.lightBlue)
        txt.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_demibold)
        txt.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        if (value != null) {
            value.visibility = View.VISIBLE
        }
    }

    /**
     * This method will change the view style if weekday("M","T","W","F"...) is selected
     */
    fun setWeekSelected(textView: TextView) {
        textView.background.setTint(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_demibold)
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    /**
     * This method will change the view style if weekday("M","T","W","F"...) is unselected
     */
    private fun setWeekUnSelected(textView: TextView) {
        textView.background.setTint(ContextCompat.getColor(requireContext(), R.color.lightBlue))
        textView.typeface = ResourcesCompat.getFont(requireContext(), R.font.avenirnext_regular)
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.darkGrey))
    }

    override fun onClick(v: View?) {
        /**
         * if tag is 0 weekday is not selected, tag 1 weekday is selected
         */
        if (v != null) {
            if (v.tag == "0") {
                v.tag = "1"
                setWeekSelected(v as TextView)
            } else {
                v.tag = "0"
                setWeekUnSelected(v as TextView)
            }

        }
    }

    override fun onWheelScrolled(offset: Int) {
//        Log.d(TAG, "onWheelScrolled: $offset")
    }

    /**
     * Wheel Scroll Change Listener
     */
    override fun onWheelSelected(position: Int) {
        Log.d(TAG, "onWheelSelected: position $position")
        wheelPosition = position
        when (position) {
            0 -> {
                // Every Day is selected
                binding.monthLayout.visibility = View.GONE
                binding.weekLayout.visibility = View.GONE
                binding.yearLayout.visibility = View.GONE
            }
            1 -> {
                // Week is selected
                binding.monthLayout.visibility = View.GONE
                binding.yearLayout.visibility = View.GONE
                binding.weekLayout.visibility = View.VISIBLE
            }
            2 -> {
                // Month is selected
                binding.weekLayout.visibility = View.GONE
                binding.yearLayout.visibility = View.GONE
                binding.monthLayout.visibility = View.VISIBLE
            }
            3 -> {
                // Year is selected
                binding.weekLayout.visibility = View.GONE
                binding.monthLayout.visibility = View.GONE
                binding.yearLayout.visibility = View.VISIBLE

            }
            else -> {
                // By Default Every day is selected
                binding.weekLayout.visibility = View.GONE
                binding.monthLayout.visibility = View.GONE
                binding.yearLayout.visibility = View.GONE
            }
        }

    }

    override fun onWheelScrollStateChanged(state: Int) {
//        Log.d(TAG, "onWheelScrollStateChanged: $state")
    }

    /**
     * The below method will check if week repeat option is selected but no weekday("M","T","W","F"...) is selected
     * for repeat it will show error
     */
    private fun weekValidation(): Boolean{
        if(binding.mon.tag == "0" && binding.tue.tag == "0" && binding.wed.tag == "0" && binding.thu.tag == "0" &&
            binding.fri.tag == "0" && binding.sat.tag == "0" && binding.sun.tag == "0"){

            Toast.makeText(context?.applicationContext, context?.getString(R.string.week_error), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /**
     * This will add selected weekdays to arraylist
     */
    private fun getWeekDays(){
        arrayList.clear()
        if(binding.mon.tag == "1") arrayList.add("Mon")
        if(binding.tue.tag == "1") arrayList.add("Tue")
        if(binding.wed.tag == "1") arrayList.add("Wed")
        if(binding.thu.tag == "1") arrayList.add("Thu")
        if(binding.fri.tag == "1") arrayList.add("Fri")
        if(binding.sat.tag == "1") arrayList.add("Sat")
        if(binding.sun.tag == "1") arrayList.add("Sun")
    }

}
