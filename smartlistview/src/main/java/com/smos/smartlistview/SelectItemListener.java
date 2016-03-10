package com.smos.smartlistview;

import android.graphics.Point;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by zhangjunxing on 16-2-26.
 */
public class SelectItemListener implements RecyclerView.OnItemTouchListener {

    private final SelectScroller mSelectScroller = new SelectScroller();
    private RecyclerView mRecyclerView;
    RecyclerView.ViewHolder mSelected = null;
    private Point mDownPoint = new Point();
    private Point mCurrPoint = new Point();

    private int mSelectState = SELECT_IDLE;
    private static final int SELECT_IDLE = 0;
    private static final int SELECT_MOVING = 1;
    private OnSelectListener mOnSelectListener;
    private boolean mDownItemChecked = false;
    private int mPrePosition = -1;
    private final int[] mTempLoc = new int[2];
    private float mScrollStartY;

    public interface OnSelectListener {
        void onSelect(int position, boolean flag);

        boolean isSelected(int position);
    }

    public SelectItemListener(RecyclerView recyclerView, OnSelectListener l) {
        mRecyclerView = recyclerView;
        mOnSelectListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        int position = startCheckPosition(event);
        if (action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (position >= 0) {
                mScrollStartY = getScrollStartY();
                mSelected = mRecyclerView.getChildViewHolder(findChildView(event));
                mDownItemChecked = mOnSelectListener.isSelected(position);
                continueSelectMove(position);
                mDownPoint.set(x, y);
                return true;
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mPrePosition = -1;
            mSelected = null;
        }
        if (position < 0) {
            return false;
        } else {
            return mSelected != null;
        }
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        int position = startCheckPosition(event);
        RecyclerView.ViewHolder viewHolder = mSelected;
        if (viewHolder == null) {
            return;
        }
        if (position >= 0 && action == MotionEvent.ACTION_MOVE) {
            mSelectState = SELECT_MOVING;
        }
        if (mSelectState == SELECT_MOVING) {
            onMoveTouchEvent(event, position);
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        return;
    }

    //FIXME getItemCount 和 getChildCount的 区别
    private int startCheckPosition(MotionEvent ev) {
        int checkBoxId = R.id.checkbox;
        View child = findChildView(ev);
        int touchPos = mRecyclerView.getChildLayoutPosition(child);
        final int count = mRecyclerView.getLayoutManager().getItemCount();
        if (touchPos >= 0 && touchPos < count) {
            final int rawX = (int) ev.getRawX();
            final int rawY = (int) ev.getRawY();

            View dragBox = child.findViewById(checkBoxId);
            if (dragBox != null) {
                dragBox.getLocationOnScreen(mTempLoc);

                if (rawX > mTempLoc[0] && rawY > mTempLoc[1]
                        && rawX < mTempLoc[0] + dragBox.getWidth()
                        && rawY < mTempLoc[1] + dragBox.getHeight()) {
                    return touchPos;
                }
            }
        }
        return -1;
    }

    private void onMoveTouchEvent(MotionEvent event, int position) {
        int action = MotionEventCompat.getActionMasked(event);
        int x = (int) event.getX();
        int y = (int) event.getY();
        mCurrPoint.set(x, y);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mPrePosition = -1;
                mSelectScroller.stopScroll();
                break;
            case MotionEvent.ACTION_MOVE:
                continueSelect(x, y, position);
                break;
        }
    }

    private void continueSelect(int x, int y, int position) {
        int deltaY = y - mDownPoint.y;
        if (y > mScrollStartY && deltaY > 0) {
            mSelectScroller.startScroll(SelectScroller.DOWN);
            return;
        } else if (y < mScrollStartY && deltaY < 0) {
            mSelectScroller.startScroll(SelectScroller.UP);
            return;
        } else if (y == mScrollStartY) {
            mSelectScroller.stopScroll();
        }
        continueSelectMove(position);
    }

    private boolean continueSelectMove(int position) {
        if (position < 0 || position == mPrePosition) {
            return false;
        } else {
            mPrePosition = position;
            if (mOnSelectListener != null) {
                mOnSelectListener.onSelect(position, !mDownItemChecked);
            }
        }
        return true;
    }

    public int getScrollStartY() {
        LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int padTop = lm.getPaddingTop();
        int padBottom = lm.getPaddingBottom();
        int height = lm.getHeight();
        int listHeight = height - padTop - padBottom;
        return listHeight / 2;
    }

    private View findChildView(MotionEvent event) {
        return mRecyclerView.findChildViewUnder(event.getX(), event.getY());
    }

    private final class SelectScroller implements Runnable {

        private boolean mAbort;
        private int mScrollDir;
        private boolean mScrolling = false;
        private float mScrollSpeed;
        private final float SPEED_CARDINAL = 50;
        private boolean mIsAllowToMove = true;

        public static final int UP = 1;
        public static final int DOWN = 2;

        public void startScroll(int dir) {
            if (!mScrolling) {
                mScrolling = true;
                mAbort = false;
                mScrollDir = dir;
                mRecyclerView.post(this);
            }
        }

        public void stopScroll() {
            mRecyclerView.removeCallbacks(this);
            mScrolling = false;
        }

        @Override
        public void run() {
            if (mAbort || !mIsAllowToMove) {
                return;
            }
            LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            int first = lm.findFirstVisibleItemPosition();
            int last = lm.findLastVisibleItemPosition();
            int count = lm.getItemCount();
            int padTop = lm.getPaddingTop();
            int height = lm.getHeight();
            int padBottom = lm.getPaddingBottom();
            final int listHeight = height - padTop - padBottom;

            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    performSelectAction();
                }
            });

            if (mScrollDir == UP) {
                View v = mRecyclerView.getChildAt(0);
                if (v == null || (first == 0 && v.getTop() == padTop)) {
                    mScrolling = false;
                    return;
                }

                int yDelta = mCurrPoint.y < padTop ? padTop : mCurrPoint.y;
                mScrollSpeed = (yDelta - mScrollStartY) / mScrollStartY * SPEED_CARDINAL;
            } else {
                View v = mRecyclerView.getChildAt(last - first);
                if (v == null || (last == count - 1 && v.getBottom() <= listHeight + padTop)) {
                    mScrolling = false;
                    return;
                }

                int y = height - padBottom;
                int yDelta = mCurrPoint.y > y ? y : mCurrPoint.y;
                mScrollSpeed = (yDelta - mScrollStartY) / mScrollStartY * SPEED_CARDINAL;
            }

            mRecyclerView.scrollBy(0, (int) mScrollSpeed);
            mRecyclerView.invalidate();
            mRecyclerView.post(this);
        }

        /**
         * select or unSelect the item when scrolling.
         */
        private void performSelectAction() {
            LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
            if (mCurrPoint.y < lm.getPaddingTop()) {
                continueSelectMove(lm.findFirstVisibleItemPosition());
                return;
            } else if (mCurrPoint.y > (lm.getHeight() - lm.getPaddingBottom())) {
                continueSelectMove(lm.findLastVisibleItemPosition());
                return;
            }
            View child = mRecyclerView.findChildViewUnder(mCurrPoint.x, mCurrPoint.y);
            int position = mRecyclerView.getChildLayoutPosition(child);
            if (child != null) {
                int checkBoxId = R.id.checkbox;
                View cb = child.findViewById(checkBoxId);
                if (cb != null) {
                    continueSelectMove(position);
                }
            }
        }

    }
}
