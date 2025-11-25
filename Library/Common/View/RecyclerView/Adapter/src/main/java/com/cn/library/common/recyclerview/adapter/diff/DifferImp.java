package com.cn.library.common.recyclerview.adapter.diff;

import androidx.annotation.NonNull;
import com.cn.library.common.recyclerview.adapter.diff.ListChangeListener;

/**
 * 使用java接口定义方法
 * @param <T>
 */
public interface DifferImp<T> {
    void addListListener(@NonNull ListChangeListener<T> listChangeListener);
}
