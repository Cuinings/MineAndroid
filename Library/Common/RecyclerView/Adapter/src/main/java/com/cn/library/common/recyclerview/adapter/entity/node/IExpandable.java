package com.cn.library.common.recyclerview.adapter.entity.node;

import java.util.List;

/**
 * implement the interface if the item is expandable
 */
public interface IExpandable<T> {
    boolean isExpanded();
    void setExpanded(boolean expanded);
    List<T> getSubItems();

    /**
     * Get the level of this item. The level start from 0.
     * If you don't care about the level, just return a negative.
     */
    int getLevel();
}
