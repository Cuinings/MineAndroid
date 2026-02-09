package com.cn.core.ui.view.recyclerview.adapter.listener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;

public interface GridSpanSizeLookup {

    int getSpanSize(@NonNull GridLayoutManager gridLayoutManager, int viewType, int position);
}
