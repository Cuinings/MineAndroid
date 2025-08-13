package com.cn.mine.wan.android.app.page.login

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.cn.library.commom.viewmodel.BasicViewModel
import com.cn.library.commom.viewmodel.UIEvent
import com.cn.library.commom.viewmodel.UIState
import com.cn.library.common.flow.collectByScope
import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.UserEntity
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.login.LoginEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/8/13 10:13
 * @Description:
 */
@HiltViewModel
class LoginActivityViewModel @Inject constructor(): BasicViewModel<LoginActivityState, LoginActivityEvent>() {

    @Inject lateinit var loginEvent: LoginEvent

    override fun initUIState(): LoginActivityState = LoginActivityState.Init

    override fun handleEvent(event: LoginActivityEvent) {
        when(event) {
            is LoginActivityEvent.Login -> viewModelScope.launch {
                loginEvent.execute(LoginEvent.LoginParam(
                    event.username,
                    event.password
                )).collectByScope(viewModelScope) {
                    Log.d(TAG, "loginEvent: $it")
                    when(it) {
                        is EventResult.Failed -> TODO()
                        is EventResult.Loading -> TODO()
                        is EventResult.Success<CommonEntity<UserEntity>> -> sendUiState { LoginActivityState.LoginSuccess(it.result?.data) }
                    }

                }
            }
        }
    }
}

sealed class LoginActivityState: UIState {
    object Init: LoginActivityState()
    data class LoginSuccess(val entity: UserEntity?): LoginActivityState()
}
sealed class LoginActivityEvent: UIEvent {
    data class Login(val username: String, val password: String): LoginActivityEvent()
}