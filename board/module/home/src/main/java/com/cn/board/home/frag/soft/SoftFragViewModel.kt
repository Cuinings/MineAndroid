package com.cn.board.home.frag.soft

import android.view.View
import androidx.lifecycle.viewModelScope
import com.cn.board.home.data.appInfoData
import com.cn.board.home.domain.usecase.UseCaseFactory
import com.cn.board.home.entity.SoftEntity
import com.cn.board.home.room.repsoitory.repository
import com.cn.board.home.state.State.isOpenAPS
import com.cn.core.ui.viewmodel.BasicViewModel
import com.cn.board.home.function.IntLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoftFragViewModel: BasicViewModel() {

    val mainEditVis = IntLiveData().default(View.GONE)
    val notifyItemIndex = IntLiveData()
    val tvAppListState = IntLiveData().default(View.VISIBLE)

    /**
     * 更新首页应用排序。
     * 替代原 [UpdateMainAppHandler]。
     */
    fun updateMainAppOrder(changedList: ArrayList<SoftEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            appInfoData.mainAppData.run {
                clear()
                changedList.forEach { entity ->
                    if (entity.appInfo?.appType != com.cn.board.home.entity.EmAppType.NONE) {
                        add(entity)
                        appInfoData.update(entity)
                    }
                }
            }
            // 通过 UseCase 持久化重排结果
            UseCaseFactory.manageHomeAppOrder.reorderHomeApps(changedList)
        }
    }

    fun notify(entity: SoftEntity) {
        appInfoData.appData.let {
            for(index in 0 until it.toList().size) {
                if (it[index].appInfo?.id == entity.appInfo?.id) {
                    val bLogin = isOpenAPS
                    if (bLogin) it[index].appInfo?.main = entity.appInfo?.main?:0
                    else it[index].appInfo?.offlineMain = entity.appInfo?.offlineMain?:0
                    notifyItemIndex.value = index
                    return
                }
            }
        }
    }

    suspend fun extractedSortApp(startIndex: Int, endIndex: Int, data: MutableList<SoftEntity>) {
        var id: Int
        if (startIndex < endIndex) {
            for (index in endIndex downTo startIndex + 1) {
                id = data[index].appInfo?.id?:0
                data[index].appInfo?.id = data[index - 1].appInfo?.id?:0
                data[index - 1].appInfo?.id = id
            }
            for (index in startIndex .. endIndex) {
                repository.update(data[index].appInfo)
            }
        } else if (startIndex > endIndex) {
            for (index in startIndex.coerceAtMost(endIndex) until startIndex.coerceAtLeast(endIndex)) {
                id = data[index].appInfo?.id?:0
                data[index].appInfo?.id = data[index + 1].appInfo?.id?:0
                data[index + 1].appInfo?.id = id
            }
            for (index in endIndex .. startIndex) {
                repository.update(data[index].appInfo)
            }
        }
    }
}