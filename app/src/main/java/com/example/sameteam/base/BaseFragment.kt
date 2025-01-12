package com.example.sameteam.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleObserver
import com.example.sameteam.BR
import com.example.sameteam.MyApplication
import kotlinx.coroutines.CoroutineScope

abstract class BaseFragment<T : ViewDataBinding> : Fragment(), LifecycleObserver {
    abstract fun layoutID(): Int

    abstract fun viewModel(): BaseViewModel

    abstract fun initFragment(mBinding: ViewDataBinding)

    lateinit var mBinding: ViewDataBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = DataBindingUtil.inflate<T>(inflater, layoutID(), container, false)
        mBinding.lifecycleOwner = this
        mBinding.setVariable(BR.viewModel, viewModel())
//        binding.setVariable(BR.handler, this)
        initFragment(mBinding)
        return mBinding.root
    }


    protected fun showMessage(message: String) {
        Toast.makeText(MyApplication.getInstance(), message, Toast.LENGTH_LONG).show()
    }

    fun getViewModel() = viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun startActivity(cls: Class<*>) {
        getAct().startActivity(cls)
    }

    private fun getAct(): BaseActivity<*> {
          return activity as BaseActivity<*>
    }

}