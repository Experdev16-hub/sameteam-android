package com.example.sameteam.homeScreen.bottomNavigation.taskModule.bottomSheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.BottomSheetParticipantsBinding
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.bottomNavigation.taskModule.model.TaskParticipantsModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ParticipantsBottomSheet(val mContext: Context, val participants: ArrayList<TaskParticipantsModel>) : BottomSheetDialogFragment() {

    private val TAG = "ParticipantsBottomSheet"

    lateinit var binding: BottomSheetParticipantsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_participants, container, false)

        /**
         * This will expand the bottom sheet up to maximum height needed for bottom sheet,
         * if height is more, then scroll will be there
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

        val usersList = ArrayList<UserModel>()
        for(temp in participants){
            var userModel:UserModel= UserModel()
            userModel= temp.user!!
            userModel.response=temp.response
            usersList.add(userModel)
        }

        binding.usersRecView.layoutManager = LinearLayoutManager(mContext)
        binding.usersRecView.adapter = UserDirectoryAdapter(mContext, usersList, ArrayList(), ArrayList())

        binding.btnCross.setOnClickListener {
            dismiss()
        }
    }
}