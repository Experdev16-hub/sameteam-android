package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetRepeatTaskBinding
import com.example.sameteam.databinding.RemindMeCardLayoutBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate

class RepeatTaskBottomSheet(val localDate: LocalDate) : BottomSheetDialogFragment() {

    //Implemented in CreateTaskActivity
    interface NeverClickedListener{
        fun onNeverClicked()
    }

    lateinit var neverClickedListener: NeverClickedListener

    lateinit var binding:BottomSheetRepeatTaskBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_repeat_task,container,false)

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


        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnCross.setOnClickListener {
            dismiss()
        }

        //For Custom Repetition
        binding.btnCustom.setOnClickListener {
            val fragment = CustomRepeatTaskBottomSheet(localDate)
            fragment.show(requireActivity().supportFragmentManager,CustomRepeatTaskBottomSheet::class.java.name)
        }

        binding.btnNever.setOnClickListener {
            neverClickedListener = requireContext() as NeverClickedListener
            neverClickedListener.onNeverClicked()
            dismiss()
        }
    }

}