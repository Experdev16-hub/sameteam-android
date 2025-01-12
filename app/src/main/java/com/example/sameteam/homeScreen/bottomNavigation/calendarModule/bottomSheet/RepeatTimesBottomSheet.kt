package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetRepeatTimesBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RepeatTimesBottomSheet : BottomSheetDialogFragment() {

    //Implemented in CreateTaskActivity
    interface TimesDoneListener{
        fun onRepeatTimesDone(value: String)
    }

    lateinit var timesDoneListener: TimesDoneListener

    lateinit var binding:BottomSheetRepeatTimesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_repeat_times,container,false)

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDone.setOnClickListener {
            if(!binding.times.text.isNullOrBlank()){
                timesDoneListener = activity as TimesDoneListener
                timesDoneListener.onRepeatTimesDone(binding.times.text.toString().trim())
            }
            dismiss()
        }

        binding.btnBack.setOnClickListener {
            dismiss()
        }
    }
}