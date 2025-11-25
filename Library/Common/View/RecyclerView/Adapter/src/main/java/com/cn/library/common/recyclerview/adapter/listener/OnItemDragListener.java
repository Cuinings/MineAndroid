package com.cn.library.common.recyclerview.adapter.listener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public interface OnItemDragListener {

    boolean onItemAllowMove(RecyclerView.ViewHolder viewHolder, int pos);
    void onItemDragStart(RecyclerView.ViewHolder viewHolder, int pos);

    void onItemDragMoving(RecyclerView.ViewHolder source, int from, RecyclerView.ViewHolder target, int to);

    void onItemDragEnd(RecyclerView.ViewHolder viewHolder, int pos);

    //拖动的Item移动距离改变监听，dx,dy分别为x和y方向的移动距离，初始值0，该函数内尽量不要做耗时操作来源为onChildDraw，耗时操作会影响拖动流畅度
    void onItemDragMovePos( @NonNull RecyclerView.ViewHolder viewHolder,float dX, float dY,int actionState, boolean isCurrentlyActive);
}
