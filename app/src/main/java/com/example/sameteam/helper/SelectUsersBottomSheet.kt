package com.example.sameteam.helper

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.databinding.BottomSheetInvitePeopleBinding
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.drawerNavigation.activity.AddEventActivity
import com.example.sameteam.homeScreen.drawerNavigation.viewModel.AddEventVM
import com.example.sameteam.widget.ConfirmDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectedUsersBottomSheet(
    val users: ArrayList<UserModel>,
    var selectedUsers: ArrayList<UserModel>,
) : BottomSheetDialogFragment() {
    private val TAG = "InvitePeopleBottomSheet"
    private lateinit var addEventVM: AddEventVM

    //Implemented in Activity
    interface AllPeopleSelectedListener {
        fun onAllSelected(tag: Boolean)
    }

    interface OnUserSearchedListener {
        fun onUserSearched(search_key: String)
    }

    lateinit var allPeopleSelectedListener: AllPeopleSelectedListener
    lateinit var onUserSearchedListener: OnUserSearchedListener

    lateinit var binding: BottomSheetInvitePeopleBinding
    var usersData = ArrayList<UserModel>()
    var searchList = ArrayList<UserModel>()
    var isSearch = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_invite_people, container, false)

        addEventVM = ViewModelProvider(requireActivity()).get(AddEventVM::class.java)

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

        usersData = users

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userRecView.adapter =
            UserDirectoryAdapter(requireContext(), usersData, selectedUsers, ArrayList())
        binding.scrollLayout.visibility = View.INVISIBLE

        if (activity !is AddEventActivity) {
            binding.teamLayout.visibility = View.VISIBLE
            binding.btnSelectAll.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        binding.btnInfo.setOnClickListener {
            val confirmDialog = ConfirmDialog(
                requireActivity(),
                " Create a team to share tasks with in the Communications menu.", "AlreadyCompleted"
            )
            confirmDialog.show(requireActivity().supportFragmentManager, "Confirm")
        }

        /**
         * Invite all users button
         */
        binding.btnSelectAll.setOnClickListener {
            allPeopleSelectedListener = activity as AllPeopleSelectedListener

            if (selectedUsers.size == usersData.size)
                allPeopleSelectedListener.onAllSelected(false)
            else
                allPeopleSelectedListener.onAllSelected(true)
        }


        binding.btnDone.setOnClickListener {
            dismiss()
        }

        addEventVM.observedChanges().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                when (it) {
                    Constants.VISIBLE -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    Constants.HIDE -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        addEventVM.observeUsers().observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                if (it.isNotEmpty()) {

                    isSearch = true
                    binding.teamLayout.visibility = View.GONE

                    searchList = it
                    binding.userRecView.adapter =
                        UserDirectoryAdapter(
                            requireContext(),
                            searchList,
                            selectedUsers,
                            ArrayList()
                        )
                    (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()

                }
            }
        }

        /**
         * Observe changes made by UserDirectoryAdapter in inviting users
         */

        if (activity is AddEventActivity)
            (activity as AddEventActivity).addEventVM.observeSelectedUsers()
                .observe(this) { event ->
                    event?.getContentIfNotHandled()?.let {
                        selectedUsers = it
                        if (isSearch)
                            binding.userRecView.adapter = UserDirectoryAdapter(
                                requireContext(),
                                searchList,
                                selectedUsers,
                                ArrayList()
                            )
                        else
                            binding.userRecView.adapter = UserDirectoryAdapter(
                                requireContext(),
                                usersData,
                                selectedUsers,
                                ArrayList()
                            )


                        (binding.userRecView.adapter as UserDirectoryAdapter).notifyDataSetChanged()
                    }
                }

        /**
         * Edit text change listener for search functionality
         */
        binding.txtInputSearch.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank())
                    addEventVM.getUserList()
                else
                    addEventVM.getUserList(s.toString())
            }

        })
    }
}