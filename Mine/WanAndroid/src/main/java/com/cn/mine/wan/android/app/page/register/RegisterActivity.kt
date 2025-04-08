package com.cn.mine.wan.android.app.page.register

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.databinding.ActivityLoginBinding
import com.cn.mine.wan.android.databinding.ActivityRegisterBinding
import com.cn.mine.wan.android.net.WanAndroidAPI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/4/7 14:43
 * @Description:
 */
@AndroidEntryPoint
class RegisterActivity: BasicVBActivity<ActivityRegisterBinding>({ ActivityRegisterBinding.inflate(it) }) {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    private val loginActivityClick = LoginActivityClick()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.register.setOnClickListener { loginActivityClick.register() }
    }

    inner class LoginActivityClick {
        fun register() { lifecycleScope.launch {
            checkParamRegister { username, password, rePassword ->
                wanAndroidAPI.register(username, password, rePassword).result({
                    finish()
                }) {
                    Toast.makeText(this@RegisterActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        } }
    }


    suspend fun checkParamRegister(action: suspend (String, String, String) -> Unit) {
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        val rePassword = binding.rePassword.text.toString()
        if (username.isBlank()) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isBlank() || rePassword.isBlank()) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != rePassword) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
            return
        }
        action.invoke(username,  password, rePassword)
    }

}