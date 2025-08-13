package com.cn.mine.wan.android.events.login

import com.cn.mine.wan.android.entity.CommonEntity
import com.cn.mine.wan.android.entity.UserEntity
import com.cn.mine.wan.android.events.EventParam
import com.cn.mine.wan.android.events.EventResult
import com.cn.mine.wan.android.events.IEventWithParam
import com.cn.mine.wan.android.events.login.LoginEvent.LoginParam
import com.cn.mine.wan.android.repository.RepositoryExt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * @Author: CuiNing
 * @Time: 2025/8/13 12:11
 * @Description:
 */
class LoginEvent @Inject constructor(): IEventWithParam<LoginParam, EventResult<CommonEntity<UserEntity>>> {

    data class LoginParam(
        val username: String,
        val password: String,
    ) : EventParam()

    override fun execute(
        param: LoginParam
    ): Flow<EventResult<CommonEntity<UserEntity>>>  =
        callbackFlow {
            RepositoryExt.apiRepository.login(param.username, param.password).collect {
                if (it.isSuccess) trySendBlocking(EventResult.Success(it.getOrNull()))
                else trySendBlocking(EventResult.Failed(it.exceptionOrNull()))
            }
            awaitClose()
        }

}