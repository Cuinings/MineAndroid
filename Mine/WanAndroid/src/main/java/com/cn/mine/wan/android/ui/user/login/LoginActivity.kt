package com.cn.mine.wan.android.ui.user.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.cn.library.common.activity.BasicVBActivity
import com.cn.mine.wan.android.ui.main.MainActivity
import com.cn.mine.wan.android.ui.user.register.RegisterActivity
import com.cn.mine.wan.android.data.entity.result
import com.cn.mine.wan.android.databinding.ActivityLoginBinding
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
class LoginActivity: BasicVBActivity<ActivityLoginBinding>({ ActivityLoginBinding.inflate(it) }) {

    @Inject
    lateinit var wanAndroidAPI: WanAndroidAPI

    private val loginActivityClick = LoginActivityClick()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.login.setOnClickListener { loginActivityClick.login() }
        binding.register.setOnClickListener { loginActivityClick.register() }
    }

    inner class LoginActivityClick {
        fun login() { lifecycleScope.launch {
            checkParamLogin { username, password -> wanAndroidAPI.login(username, password).result({
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }) {
                Toast.makeText(this@LoginActivity, it, Toast.LENGTH_SHORT).show()
            } }
        } }

        fun register() { lifecycleScope.launch {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        } }
    }

    suspend fun checkParamLogin(action: suspend (String, String) -> Unit) {
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        if (username.isBlank()) {
            Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isBlank()) {
            Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        action.invoke(username,  password)
    }

}