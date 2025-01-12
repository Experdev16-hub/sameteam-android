package com.example.sameteam.homeScreen.bottomNavigation.chatModule.bottonSheet

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sameteam.BR
import com.example.sameteam.MyApplication
import com.example.sameteam.R
import com.example.sameteam.authScreens.model.LoginResponseModel
import com.example.sameteam.authScreens.model.UserModel
import com.example.sameteam.base.BindingAdapter
import com.example.sameteam.databinding.BottomSheetAddContactBinding
import com.example.sameteam.helper.Constants
import com.example.sameteam.helper.SharedPrefs
import com.example.sameteam.helper.Utils
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.activity.UserDirectoryActivity
import com.example.sameteam.homeScreen.bottomNavigation.chatModule.adapter.UserDirectoryAdapter
import com.example.sameteam.homeScreen.bottomNavigation.userDirectoryModule.viewModel.AllUsersFragVM
import com.example.sameteam.quickBlox.chat.ChatHelper
import com.example.sameteam.quickBlox.db.QbUsersDbManager
import com.example.sameteam.quickBlox.util.SharedPrefsHelper
import com.example.sameteam.widget.ConfirmCallDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.quickblox.core.QBEntityCallback
import com.quickblox.core.exception.QBResponseException
import com.quickblox.core.request.GenericQueryRule
import com.quickblox.core.request.QBPagedRequestBuilder
import com.quickblox.users.QBUsers
import com.quickblox.users.model.QBUser

class AddContactBottomSheet(
    private val globalUserList: ArrayList<LoginResponseModel.User>,
    private val addContactId: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val TAG = "AddContactBottomSheet"

    lateinit var allUsersFragVM: AllUsersFragVM

    lateinit var binding: BottomSheetAddContactBinding
    var usersData = ArrayList<UserModel>()
    var filteredUsersData = ArrayList<UserModel>()
    var searchList = ArrayList<UserModel>()
    var isSearch = false
    var hasMoreDialogs = true
    private var currentPage: Int = 0
    var teamContactUserList = ArrayList<LoginResponseModel.User>()

    private var errorCount: Int = 0
    private lateinit var currentUser: QBUser


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_add_contact, container, false)

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

        // usersData = users
        //filteredUsersData = usersData.filterParticipants() as ArrayList<UserModel>
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // loadUsersFromQB()
        binding.btnCross.setOnClickListener {
            dismiss()
        }

        binding.searchLayout.txtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.searchLayout.txtSearch.text.toString()
                        .isBlank()
                ) {  // if search text is empty, show all users


                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.text = getString(R.string.search_with_email)

                    binding.txtUserNotFound.visibility = View.VISIBLE

                    binding.userRecView.visibility = View.GONE

                } else {

                    val string = binding.searchLayout.txtSearch.text.toString()
                    var searchList = ArrayList<LoginResponseModel.User>()

                    if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                        searchList = ArrayList(
                            globalUserList.filter {
                                it.email.toString().contains(string, ignoreCase = true)
                            })

                    binding.userRecView.adapter =
                        BindingAdapter(layoutId = R.layout.add_contact_user_list,
                            br = BR.model,
                            list = searchList,
                            clickListener = { view, i ->
                                when (view.id) {
                                    R.id.btnAdd -> {
                                        searchList[i].id?.let {
                                            addContactId.invoke(it)
                                        }
                                    }
                                }
                            })

                    binding.progressBar.visibility = View.GONE

                    binding.txtUserNotFound.visibility =
                        if (searchList.isEmpty()) View.VISIBLE else View.GONE

                    binding.txtUserNotFound.text =
                        if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                            R.string.search_with_email
                        )

                    binding.userRecView.visibility =
                        if (searchList.isEmpty()) View.GONE else View.VISIBLE

                }
            }

        })

        binding.searchLayout.txtSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH && v.text.isNotBlank()) {

                /**
                 * Search if firstname or lastname contains matching search string OR email matches search string
                 */

                val string = binding.searchLayout.txtSearch.text.toString()
                var searchList = ArrayList<LoginResponseModel.User>()

                if (Patterns.EMAIL_ADDRESS.matcher(string).matches())
                    searchList = ArrayList(
                        globalUserList.filter {
                            it.email.toString().contains(string, ignoreCase = true)
                        })


                binding.userRecView.adapter =
                    BindingAdapter(layoutId = R.layout.add_contact_user_list,
                        br = BR.model,
                        list = searchList,
                        clickListener = { view, i ->
                            when (view.id) {
                                R.id.btnAdd -> {
                                    searchList[i].id?.let {
                                        addContactId.invoke(it)
                                    }
                                }
                            }
                        })

                binding.progressBar.visibility = View.GONE

                binding.txtUserNotFound.visibility =
                    if (searchList.isEmpty()) View.VISIBLE else View.GONE

                binding.txtUserNotFound.text =
                    if (searchList.isEmpty()) getString(R.string.participant_not_found) else getString(
                        R.string.search_with_email
                    )

                binding.userRecView.visibility =
                    if (searchList.isEmpty()) View.GONE else View.VISIBLE

                true
            } else false
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(MyApplication.getInstance(), message, Toast.LENGTH_LONG).show()

    }

    private fun loadUsersFromQB() {
        binding.progressBar.visibility = View.VISIBLE
        currentPage += 1

        val rules = ArrayList<GenericQueryRule>()
        rules.add(GenericQueryRule(Constants.ORDER_RULE, Constants.ORDER_VALUE_UPDATED_AT))

        val requestBuilder = QBPagedRequestBuilder()
        requestBuilder.rules = rules
        requestBuilder.perPage = Constants.USERS_PAGE_SIZE
        requestBuilder.page = currentPage


    }

}