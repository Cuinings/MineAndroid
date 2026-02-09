package com.cn.core.ui.view.recyclerview.adapter.listener;

import android.view.View;

import androidx.annotation.NonNull;

import com.cn.core.ui.view.recyclerview.adapter.BaseQuickAdapter;

public interface OnItemChildClickListener {
    /**
     * callback method to be invoked when an item child in this view has been click
     *
     * @param adapter  BaseQuickAdapter
     * @param view     The view whihin the ItemView that was clicked
     * @param position The position of the view int the adapter
     */
    void onItemChildClick(@NonNull BaseQuickAdapter<?,?> adapter, @NonNull View view, int position);
}
