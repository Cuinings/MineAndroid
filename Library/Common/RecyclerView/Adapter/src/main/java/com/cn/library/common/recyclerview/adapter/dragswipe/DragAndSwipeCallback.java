package com.cn.library.common.recyclerview.adapter.dragswipe;

import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.cn.library.common.recyclerview.adapter.BaseQuickAdapter;
import com.cn.library.common.recyclerview.adapter.R;
import com.cn.library.common.recyclerview.adapter.module.BaseDraggableModule;

public class DragAndSwipeCallback extends ItemTouchHelper.Callback {


    private BaseDraggableModule mDraggableModule;
    private float                    mMoveThreshold = 0.1f;
    private float                    mSwipeThreshold = 0.7f;


    private int mDragMoveFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
    private int mSwipeMoveFlags = ItemTouchHelper.END;


    public DragAndSwipeCallback(BaseDraggableModule draggableModule) {
        mDraggableModule = draggableModule;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        if (mDraggableModule != null) {
            return mDraggableModule.isDragEnabled() && !mDraggableModule.hasToggleView();
        }
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        if (mDraggableModule != null) {
            return mDraggableModule.isSwipeEnabled();
        }
        return false;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        Log.d("DragAndSwipeCallback", "onSelectedChanged: " + actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                && !isViewCreateByAdapter(viewHolder)) {
            if (mDraggableModule != null) {
                mDraggableModule.onItemDragStart(viewHolder);
            }
            viewHolder.itemView.setTag(R.id.BaseQuickAdapter_dragging_support, true);
        } else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE
                && !isViewCreateByAdapter(viewHolder)) {
            if (mDraggableModule != null) {
                mDraggableModule.onItemSwipeStart(viewHolder);
            }
            viewHolder.itemView.setTag(R.id.BaseQuickAdapter_swiping_support, true);
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (isViewCreateByAdapter(viewHolder)) {
            return;
        }

        if (viewHolder.itemView.getTag(R.id.BaseQuickAdapter_dragging_support) != null
                && (Boolean) viewHolder.itemView.getTag(R.id.BaseQuickAdapter_dragging_support)) {
            if (mDraggableModule != null) {
                mDraggableModule.onItemDragEnd(viewHolder);
            }
            viewHolder.itemView.setTag(R.id.BaseQuickAdapter_dragging_support, false);
        }
        if (viewHolder.itemView.getTag(R.id.BaseQuickAdapter_swiping_support) != null
                && (Boolean) viewHolder.itemView.getTag(R.id.BaseQuickAdapter_swiping_support)) {
            if (mDraggableModule != null) {
                mDraggableModule.onItemSwipeClear(viewHolder);
            }
            viewHolder.itemView.setTag(R.id.BaseQuickAdapter_swiping_support, false);
        }
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder current, @NonNull RecyclerView.ViewHolder target) {
        if (null != mDraggableModule)
            return mDraggableModule.onItemAllowMove(target);
        return super.canDropOver(recyclerView, current, target);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (isViewCreateByAdapter(viewHolder) || (null != mDraggableModule && Boolean.FALSE.equals(mDraggableModule.onItemAllowMove(viewHolder)))) {
            return makeMovementFlags(0, 0);
        }
        return makeMovementFlags(mDragMoveFlags, mSwipeMoveFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, @NonNull RecyclerView.ViewHolder target) {
        Log.d("DragAndSwipeCallback","----onMove  ");
        return source.getItemViewType() == target.getItemViewType();
    }

    @Override
    public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
        super.onMoved(recyclerView, source, fromPos, target, toPos, x, y);
        Log.d("DragAndSwipeCallback","----onMoved:x = "+x+", y = "+y);
        if (mDraggableModule != null) {
            mDraggableModule.onItemDragMoving(source, target);
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        Log.d("DragAndSwipeCallback", "onSwiped: ");
        if (!isViewCreateByAdapter(viewHolder)) {
            if (mDraggableModule != null) {
                mDraggableModule.onItemSwiped(viewHolder);
            }
        }
    }

    @Override
    public float getMoveThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return mMoveThreshold;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return mSwipeThreshold;
    }

    /**
     * Set the fraction that the user should move the View to be considered as swiped.
     * The fraction is calculated with respect to RecyclerView's bounds.
     * <p>
     * Default value is .5f, which means, to swipe a View, user must move the View at least
     * half of RecyclerView's width or height, depending on the swipe direction.
     *
     * @param swipeThreshold A float value that denotes the fraction of the View size. Default value
     *                       is .8f .
     */
    public void setSwipeThreshold(float swipeThreshold) {
        mSwipeThreshold = swipeThreshold;
    }


    /**
     * Set the fraction that the user should move the View to be considered as it is
     * dragged. After a view is moved this amount, ItemTouchHelper starts checking for Views
     * below it for a possible drop.
     *
     * @param moveThreshold A float value that denotes the fraction of the View size. Default value is
     *                      .1f .
     */
    public void setMoveThreshold(float moveThreshold) {
        mMoveThreshold = moveThreshold;
    }

    /**
     * <p>Set the drag movement direction.</p>
     * <p>The value should be ItemTouchHelper.UP, ItemTouchHelper.DOWN, ItemTouchHelper.LEFT, ItemTouchHelper.RIGHT or their combination.</p>
     * You can combine them like ItemTouchHelper.UP | ItemTouchHelper.DOWN, it means that the item could only move up and down when dragged.
     *
     * @param dragMoveFlags the drag movement direction. Default value is ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT.
     */

    public void setDragMoveFlags(int dragMoveFlags) {
        mDragMoveFlags = dragMoveFlags;
    }

    /**
     * <p>Set the swipe movement direction.</p>
     * <p>The value should be ItemTouchHelper.START, ItemTouchHelper.END or their combination.</p>
     * You can combine them like ItemTouchHelper.START | ItemTouchHelper.END, it means that the item could swipe to both left or right.
     *
     * @param swipeMoveFlags the swipe movement direction. Default value is ItemTouchHelper.END.
     */
    public void setSwipeMoveFlags(int swipeMoveFlags) {
        mSwipeMoveFlags = swipeMoveFlags;
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE
                && !isViewCreateByAdapter(viewHolder)) {
            View itemView = viewHolder.itemView;

            c.save();
            if (dX > 0) {
                c.clipRect(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + dX, itemView.getBottom());
                c.translate(itemView.getLeft(), itemView.getTop());
            } else {
                c.clipRect(itemView.getRight() + dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
                c.translate(itemView.getRight() + dX, itemView.getTop());
            }
            if (mDraggableModule != null) {
                mDraggableModule.onItemSwiping(c, viewHolder, dX, dY, isCurrentlyActive);
            }
            c.restore();

        }
    }

    private boolean isViewCreateByAdapter(@NonNull RecyclerView.ViewHolder viewHolder) {
        int type = viewHolder.getItemViewType();
        return type == BaseQuickAdapter.HEADER_VIEW || type == BaseQuickAdapter.LOAD_MORE_VIEW
                || type == BaseQuickAdapter.FOOTER_VIEW || type == BaseQuickAdapter.EMPTY_VIEW;
    }

    @Override
    public int interpolateOutOfBoundsScroll(@NonNull RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
        return super.interpolateOutOfBoundsScroll(recyclerView, viewSize, viewSizeOutOfBounds, totalSize, msSinceStartScroll);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
//        Log.d("DragAndSwipeCallback","----onChildDraw  dX = "+dX+", dY = "+dY+",actionState = "+actionState+",isCurrentlyActive = "+isCurrentlyActive);
        if (mDraggableModule != null) {
            mDraggableModule.onItemDragMovePos(viewHolder, dX,dY,actionState,isCurrentlyActive);
        }
    }
}
