package com.cn.board.meet.home.app

import android.view.View
import com.cn.board.meet.home.databinding.FragmentAppAggregatorBinding
import com.cn.core.ui.fragment.BasicVmDBFragment

/**
 * @author: cn
 * @time: 14/7/2026 下午 3:10
 * @history
 * @description:
 */
class AppAggregatorFrag: BasicVmDBFragment<AppAggregatorFragModel, FragmentAppAggregatorBinding>(
    { viewModelProvider -> viewModelProvider[AppAggregatorFragModel::class.java] },
    { FragmentAppAggregatorBinding.inflate(it) }
) {

    override fun onBindViewModel() {
    }

    override fun onBindData() {
    }

    override fun onObserver() {
    }

    inner class AAppAggregatorFragClick {

        fun manager(view: View) {

        }
    }
}