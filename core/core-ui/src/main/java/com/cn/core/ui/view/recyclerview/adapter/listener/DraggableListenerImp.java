package com.cn.core.ui.view.recyclerview.adapter.listener;

import androidx.annotation.Nullable;

public interface DraggableListenerImp {

    void setOnItemDragListener(@Nullable OnItemDragListener onItemDragListener);

    void setOnItemSwipeListener(@Nullable OnItemSwipeListener onItemSwipeListener);
}
