package com.example.sameteam.authScreens


import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.example.sameteam.R
import com.example.sameteam.authScreens.viewModel.CheckEmailVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.databinding.ActivityCheckEmailBinding

class CheckEmailActivity : BaseActivity<ActivityCheckEmailBinding>() {

    override fun layoutID() = R.layout.activity_check_email

    override fun viewModel() = ViewModelProvider(
        this,
        ViewModelProvider.AndroidViewModelFactory.getInstance(this.application)
    ).get(CheckEmailVM::class.java)

    lateinit var checkEmailVM: CheckEmailVM
    lateinit var binding: ActivityCheckEmailBinding

    override fun initActivity(mBinding: ViewDataBinding) {
        checkEmailVM = getViewModel() as CheckEmailVM
        binding = mBinding as ActivityCheckEmailBinding

        binding.btnBack.setOnClickListener{
            startActivity(LoginActivity::class.java)
            finish()
        }
    }

}