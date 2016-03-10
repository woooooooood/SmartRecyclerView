package com.smos.smartlistview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by nrs on 3/8/16.
 */
public class SmartListViewManager {
    public static final int SWIPE_MODE = 1;
    public static final int Select_MODE = 2;
    private static final int NORMAL_MODE = 3;
    private int mEditMode = NORMAL_MODE;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private SwipeItemListener mSwipeItemListener;
    private SelectItemListener mSelectItemListener;
    private StickyHeaderDecoration mStickyHeaderDecoration;
    private RecyclerView.ItemAnimator mItemAnimator;
    private DataSetOperator mDataSetOperator;

    private SmartListViewManager() {
    }

    public void removeItem(int position) {
        mDataSetOperator.delete(position);
        mAdapter.notifyItemRemoved(position);
    }

    public void removeItems(List<Integer> positions) {
        // Reverse-sort the list
        Collections.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return rhs - lhs;
            }
        });

        // Split the list in ranges
        while (!positions.isEmpty()) {
            if (positions.size() == 1) {
                removeItem(positions.get(0));
                positions.remove(0);
            } else {
                int count = 1;
                while (positions.size() > count && positions.get(count).equals(positions.get
                        (count - 1) - 1)) {
                    ++count;
                }
                if (count == 1) {
                    removeItem(positions.get(0));
                } else {
                    removeRange(positions.get(count - 1), count);
                }

                for (int i = 0; i < count; ++i) {
                    positions.remove(0);
                }
            }
        }
    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = positionStart+itemCount - 1; i >= positionStart; i--) {
            mDataSetOperator.delete(i);
        }
        mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
    }

    public void setEditMode(int mode) {
        mEditMode = mode;
        switch (mode) {
            case SWIPE_MODE:
                toSwipeMode();
                break;
            case Select_MODE:
                toSelectMode();
                break;
            case NORMAL_MODE:
                toNormalMode();
                break;
        }
    }

    private void toNormalMode() {
        if (mSwipeItemListener != null) {
            mSwipeItemListener.closeOpeningMenu();
        }
        mRecyclerView.removeOnItemTouchListener(mSwipeItemListener);
        mRecyclerView.removeOnItemTouchListener(mSelectItemListener);
    }

    private void toSelectMode() {
        if (mSelectItemListener == null) {
            throw new IllegalArgumentException("Not set select item listener.");
        }
        if (mSwipeItemListener != null) {
            mSwipeItemListener.closeOpeningMenu();
        }
        mRecyclerView.removeOnItemTouchListener(mSwipeItemListener);
        mRecyclerView.addOnItemTouchListener(mSelectItemListener);
    }

    private void toSwipeMode() {
        if (mSwipeItemListener == null) {
            throw new IllegalArgumentException("Not set swipe item listener.");
        }
        mRecyclerView.removeOnItemTouchListener(mSelectItemListener);
        mRecyclerView.addOnItemTouchListener(mSwipeItemListener);
    }

    public static class ManagerBuilder {
        SmartListViewManager mManager;

        public ManagerBuilder() {
            mManager = new SmartListViewManager();
        }

        public ManagerBuilder setItemAnimator(RecyclerView.ItemAnimator itemAnimator) {
            mManager.mItemAnimator = itemAnimator;
            return this;
        }

        public ManagerBuilder setRecyclerView(RecyclerView recyclerView) {
            mManager.mRecyclerView = recyclerView;
            return this;
        }

        public ManagerBuilder setAdapter(RecyclerView.Adapter adapter) {
            mManager.mAdapter = adapter;
            return this;
        }

        public ManagerBuilder setSwipeItemListener(SwipeItemListener listener) {
            mManager.mSwipeItemListener = listener;
            return this;
        }

        public ManagerBuilder setInitialEditMode(int mode) {
            mManager.mEditMode = mode;
            return this;
        }

        public ManagerBuilder setStickyHeaderDecoration(StickyHeaderDecoration decoration) {
            mManager.mStickyHeaderDecoration = decoration;
            return this;
        }

        public ManagerBuilder setSelectItemListener(SelectItemListener listener) {
            mManager.mSelectItemListener = listener;
            return this;
        }

        public ManagerBuilder setDataSetOperator(DataSetOperator operator) {
            mManager.mDataSetOperator = operator;
            return this;
        }

        public SmartListViewManager build() {
            if (mManager.mRecyclerView == null) {
                throw new IllegalArgumentException("Not set RecyclerView.");
            }
            RecyclerView recyclerView = mManager.mRecyclerView;
            Context context = recyclerView.getContext();
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if (mManager.mAdapter == null) {
                throw new IllegalArgumentException("Not set RecyclerView.Adapter.");
            }
            // set swipe item listener
            if (mManager.mSwipeItemListener != null) {
                SwipeItemListener.SwipableAdapterDecorator adapterDecorator = new
                        SwipeItemListener.SwipableAdapterDecorator(context, mManager.mAdapter);
                recyclerView.setAdapter(adapterDecorator);
            } else {
                recyclerView.setAdapter(mManager.mAdapter);
            }
            if (mManager.mStickyHeaderDecoration != null) {
                recyclerView.addItemDecoration(mManager.mStickyHeaderDecoration);
            }
            if (mManager.mItemAnimator != null) {
                recyclerView.setItemAnimator(mManager.mItemAnimator);
            }
            mManager.setEditMode(mManager.mEditMode);
            return mManager;
        }


    }
}
