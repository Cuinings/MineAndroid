package com.cn.library.common.recyclerview.adapter.listener;

import android.view.View;

import androidx.annotation.NonNull;

import com.cn.library.common.recyclerview.adapter.BaseQuickAdapter;

public interface OnItemLongClickListener {
    /**
     * callback method to be invoked when an item in this view has been
     * click and held
     *
     * @param adapter  the adapter
     * @param view     The view whihin the RecyclerView that was clicked and held.
     * @param position The position of the view int the adapter
     * @return true if the callback consumed the long click ,false otherwise
     */
    boolean onItemLongClick(@NonNull BaseQuickAdapter<?,?> adapter, @NonNull View view, int position);
}
