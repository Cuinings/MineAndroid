package com.cn.mine.wan.android.compose.ui

import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * @Author:         cn
 * @Date:           2025/4/10 16:39
 * @Description:
 */
@ExperimentalMaterial3Api
@Composable
fun MainEntrance(
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    PullToRefreshBox(false, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(modifier = modifier) {

        }
    }
}