package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetSpecificDateBinding
import com.example.sameteam.databinding.CalendarDayLayoutBinding
import com.example.sameteam.databinding.CalendarHeaderBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.Utils
import com.example.sameteam.helper.getFormattedDate
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.previous
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class SpecificDateBottomSheet(val location: String) : BottomSheetDialogFragment() {
    private val TAG = "SpecificDateBottomSheet"

    //Implemented in CreateTaskActivity
    interface SpecificDateListener {
        fun onSpecificDateDone(value: String, location: String)
    }

    lateinit var specificDateListener: SpecificDateListener

    lateinit var binding: BottomSheetSpecificDateBinding

    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    private val currentMonth: YearMonth = YearMonth.now()
    private var lastMonth: YearMonth = currentMonth.plusMonths(12)
    private val daysOfWeek: WeekFields = WeekFields.of(Locale.getDefault())
    private val firstDayOfWeek: DayOfWeek = daysOfWeek.firstDayOfWeek

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_specific_date, container, false)

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


        binding.calendarView.setup(currentMonth, lastMonth, firstDayOfWeek)
        binding.calendarView.scrollToMonth(currentMonth)


        if (location == Constants.START_DATE) {
            binding.txtDate.text =
                "Start Date" //If the sheet location is for "Task Start Date" in CreateTaskActivity
        } else {
            binding.txtDate.text =
                "Specific Date"  //If the sheet location is for "Specific End date" in CustomBottomSheet
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            if (selectedDate != null) {
                specificDateListener = activity as SpecificDateListener
//
//                val mDay = if(selectedDate!!.dayOfMonth in 1..9) "0${selectedDate!!.dayOfMonth}" else "${selectedDate!!.dayOfMonth}"
//
//                specificDateListener.onSpecificDateDone("$mDay-${selectedDate!!.month.getDisplayName(
//                    TextStyle.SHORT, Locale.ENGLISH)}-${selectedDate!!.year}",location)

                specificDateListener.onSpecificDateDone(getFormattedDate(selectedDate!!), location)

                dismiss()
//                Toast.makeText(requireContext(),"${selectedDate!!.dayOfMonth}-${selectedDate!!.month.getDisplayName(
//                    TextStyle.SHORT, Locale.ENGLISH)}-${selectedDate!!.year}" ,Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            }
        }


        /**
         * This is Date(any date in calendar view) Click Listener for the calendar
         */
        class DayViewContainer(view: View) : ViewContainer(view) {

            val dayBinding = CalendarDayLayoutBinding.bind(view)
            lateinit var day: CalendarDay
            val textView = dayBinding.calendarDayText

            init {
                textView.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        /**
                         * Only allows future dates to be clicked
                         */
                        if (day.date >= today) {
                            if (selectedDate == day.date) {
                                selectedDate = null
                                binding.calendarView.notifyDayChanged(day)
                            } else {
                                val oldDate = selectedDate
                                selectedDate = day.date
                                binding.calendarView.notifyDateChanged(day.date)
                                oldDate?.let { binding.calendarView.notifyDateChanged(oldDate) }
                            }
                        }
                    }
                }
            }


        }


        /**
         * This is to set the date view style, fully customizable
         */
        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day

                if (day.date == today && selectedDate == null)
                    selectedDate = day.date

                val textView = container.textView
                textView.text = day.date.dayOfMonth.toString()


                if (day.owner == DayOwner.THIS_MONTH) {
                    when {
                        day.date < today -> {
                            // For older dates keep the color grey
                            textView.setTextColor(ContextCompat.getColor(context!!, R.color.grey))
                            textView.background = null
                        }
                        selectedDate == day.date -> {
                            textView.setTextColor(ContextCompat.getColor(context!!, R.color.white))
                            textView.setBackgroundResource(R.drawable.selected_date_background)
                        }
                        today == day.date -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    R.color.colorPrimary
                                )
                            )
                            textView.background = null
                        }
                        else -> {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context!!,
                                    R.color.darkGrey
                                )
                            )
                            textView.background = null
                        }
                    }
                } else {
                    textView.setTextColor(ContextCompat.getColor(context!!, R.color.white))
                    textView.background = null
                }

            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = CalendarHeaderBinding.bind(view).legendLayout.root
        }

        binding.calendarView.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }
                        .forEachIndexed { index, tv ->
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        }
                    month.yearMonth
                }
            }
        }


        /**
         * Month Scroll change listener
         */
        binding.calendarView.monthScrollListener = { month ->

            // Set Month & Year name on top when scrolled
            val title = "${monthTitleFormatter.format(month.yearMonth)} ${month.yearMonth.year}"
            binding.exFiveMonthYearText.text = title

            // Add Months automatically if users keeps scrolling next month
            if (month.year == lastMonth.year) {
                lastMonth = lastMonth.plusMonths(12)
                binding.calendarView.updateMonthRange(currentMonth, lastMonth)
            }

            selectedDate?.let {
                // Clear selection if we scroll to a new month.
                selectedDate = null
                binding.calendarView.notifyDateChanged(it)
            }
        }

        /**
         * Next month button
         */
        binding.exFiveNextMonthImage.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.next)
            }
        }

        /**
         * Previous month button
         */
        binding.exFivePreviousMonthImage.setOnClickListener {
            binding.calendarView.findFirstVisibleMonth()?.let {
                binding.calendarView.smoothScrollToMonth(it.yearMonth.previous)
            }
        }

        binding.exFiveMonthYearText.setOnClickListener {
            val fragment = ChooseMonthBottomSheet { monthName ->
                run {
                    val tempMonth = Utils.getMonth(monthName)
                    binding.calendarView.smoothScrollToMonth(tempMonth)
                    binding.calendarView.notifyCalendarChanged()
                }
            }
            fragment.show(
                requireActivity().supportFragmentManager,
                ChooseMonthBottomSheet::class.java.name
            )
        }

    }

//    private fun selectDate(date: LocalDate) {
//        if (selectedDate != date) {
//            val oldDate = selectedDate
//            selectedDate = date
//            oldDate?.let { binding.calendarView.notifyDateChanged(it) }
//            binding.calendarView.notifyDateChanged(date)
////            updateAdapterForDate(date)
//        }
//    }
}