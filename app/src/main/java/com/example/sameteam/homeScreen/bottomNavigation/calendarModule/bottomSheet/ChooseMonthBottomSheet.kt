package com.example.sameteam.homeScreen.bottomNavigation.calendarModule.bottomSheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.sameteam.R
import com.example.sameteam.databinding.BottomSheetChooseMonthBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChooseMonthBottomSheet(var onItemClick: (String) -> Unit) : BottomSheetDialogFragment(), View.OnClickListener {

    lateinit var binding: BottomSheetChooseMonthBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_choose_month,container,false)

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

        binding.jan.setOnClickListener(this)
        binding.feb.setOnClickListener(this)
        binding.mar.setOnClickListener(this)
        binding.apr.setOnClickListener(this)
        binding.may.setOnClickListener(this)
        binding.jun.setOnClickListener(this)
        binding.jul.setOnClickListener(this)
        binding.aug.setOnClickListener(this)
        binding.sep.setOnClickListener(this)
        binding.oct.setOnClickListener(this)
        binding.nov.setOnClickListener(this)
        binding.dec.setOnClickListener(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCross.setOnClickListener {
            dismiss()
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            val text = (v as TextView).text
            if(!text.isNullOrBlank())
                onItemClick(text.toString())

            dismiss()
        }
    }
}