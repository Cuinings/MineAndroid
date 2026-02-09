package com.cn.core.ui.view.recyclerview.adapter.listener;

import androidx.annotation.Nullable;

/**
 * @Description: LoadMore需要设置的接口。使用java定义，以兼容java写法
 */
public interface LoadMoreListenerImp {

    void setOnLoadMoreListener(@Nullable OnLoadMoreListener listener);
}
