package com.cn.mine.wan.android.app.page.login

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicDBActivity
import com.cn.library.common.activity.BasicVBActivity
import com.cn.library.common.flow.collectByScope
import com.cn.mine.wan.android.app.databinding.ActivityLoginBinding
import com.cn.mine.wan.android.app.databinding.ActivityLoginBinding.inflate
import dagger.hilt.android.AndroidEntryPoint

/**
 * @Author: CuiNing
 * @Time: 2025/8/13 10:12
 * @Description:
 */
@AndroidEntryPoint
class LoginActivity: BasicDBActivity<ActivityLoginBinding>({ inflate(it) })  {

    private val viewModel by viewModels<LoginActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.uiStateFlow.collectByScope(lifecycleScope) {
            when(it) {
                LoginActivityState.Init -> {
//                    binding.username.setText("Cuining@1015597172")
//                    binding.password.setText("cuining")
                }
                is LoginActivityState.LoginSuccess -> {

                }
            }
        }
    }

    override fun onBindLayout() {
        binding.click = LoginActivityClick()
    }

    inner class LoginActivityClick {

        fun login(view: View) {
            viewModel.sendUIIntent(LoginActivityEvent.Login(binding.username.text.toString(), binding.password.text.toString()))
        }

    }

}