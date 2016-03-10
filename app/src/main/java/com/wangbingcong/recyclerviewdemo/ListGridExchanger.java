package com.wangbingcong.recyclerviewdemo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.smos.smartlistview.DefaultAnimListener;
import com.smos.smartlistview.TouchEventInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wangbingcong on 16-3-9.
 */
public class ListGridExchanger {

    private int mThumbViewId;
    private int mScalingViewId;
    private RecyclerView mList;
    private RecyclerView mGrid;
    private float mScaleX;
    private float mScaleY;
    private int mAnimDuration = 300;
    private AnimatorSet mAnimatorSet;
    /**
     * forbid scrolling while animating
     */
    private TouchEventInterceptor mTouchEventInterceptor;

    public ListGridExchanger(RecyclerView list, RecyclerView grid, int thumbViewId, int gridViewId) {
        mList = list;
        mGrid = grid;
        mThumbViewId = thumbViewId;
        mScalingViewId = gridViewId;

        mTouchEventInterceptor = new TouchEventInterceptor();
    }

    private void calculateScale(View thumb, View scalingView) {
        mScaleX = thumb.getWidth() / (float) scalingView.getWidth();
        mScaleY = thumb.getHeight() / (float) scalingView.getHeight();
    }

    public void swapToGrid(Animator.AnimatorListener listener) {
        if (mList == null || mGrid == null) {
            return;
        }

        mList.stopScroll();
        mGrid.stopScroll();

        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mGrid.removeOnItemTouchListener(mTouchEventInterceptor);
            mAnimatorSet.removeAllListeners();
            mAnimatorSet.cancel();
        }
        mAnimatorSet = animate(false);
        mAnimatorSet.addListener(new DefaultAnimListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mList.setVisibility(View.INVISIBLE);
                mGrid.setVisibility(View.VISIBLE);
                mGrid.addOnItemTouchListener(mTouchEventInterceptor);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mGrid.removeOnItemTouchListener(mTouchEventInterceptor);
            }
        });

        if (listener != null) {
            mAnimatorSet.addListener(listener);
        }
        mAnimatorSet.start();
    }

    public void swapToList(Animator.AnimatorListener listener) {
        if (mList == null || mGrid == null) {
            return;
        }

        mList.stopScroll();
        mGrid.stopScroll();

        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mGrid.removeOnItemTouchListener(mTouchEventInterceptor);
            mAnimatorSet.removeAllListeners();
            mAnimatorSet.cancel();
        }
        mAnimatorSet = animate(true);
        mAnimatorSet.addListener(new DefaultAnimListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mGrid.addOnItemTouchListener(mTouchEventInterceptor);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mGrid.setVisibility(View.INVISIBLE);
                mList.setVisibility(View.VISIBLE);
                mGrid.removeOnItemTouchListener(mTouchEventInterceptor);
            }
        });

        if (listener != null) {
            mAnimatorSet.addListener(listener);
        }
        mAnimatorSet.start();
    }

    /**
     * must scroll to the same position before invocation
     */
    @NonNull
    private AnimatorSet animate(final boolean toList) {
        AnimatorSet set = new AnimatorSet();
        List<Animator> anims = new ArrayList<>();

        GridLayoutManager gridlm = (GridLayoutManager) mGrid.getLayoutManager();
        int last = gridlm.findLastVisibleItemPosition();
        int first = gridlm.findFirstVisibleItemPosition();
        if (first < 0 || last < 0) {
            return set;
        }
        int gridItemCount = last - first + 1;

        List<ThumbItem> thumbItems = filterThumbItems(mList);

        int listItemCount = thumbItems.size();

        if (gridItemCount == 0 || listItemCount == 0) {
            return set;
        }

        calculateScale(thumbItems.get(0).thumb, mGrid.getChildAt(0).findViewById(mScalingViewId));

        for (int layoutPos = 0; layoutPos < gridItemCount; layoutPos++) {
            final View gridItem;
            final View expandedView;

            gridItem = mGrid.getChildAt(layoutPos);
            if (gridItem != null) {
                expandedView = gridItem.findViewById(mScalingViewId);
                if (expandedView != null) {
                    float translationX = 0;
                    float translationY = 0;
                    //　grid item would be visible after animation
                    if (layoutPos < listItemCount) {
                        View listItem = thumbItems.get(layoutPos).item;
                        View thumb = thumbItems.get(layoutPos).thumb;
                        if (listItem != null && thumb != null) {
                            translationX = getRelativeLeft(thumb, mList) - getRelativeLeft(expandedView, mGrid);
                            translationY = getRelativeTop(thumb, mList) - getRelativeTop(expandedView, mGrid);
                        }
                    }
                    //　grid item would be offscreen after animation
                    else {
                        View lastVisibleListItem = thumbItems.get(listItemCount - 1).item;
                        View thumb = thumbItems.get(listItemCount - 1).thumb;
                        if (lastVisibleListItem != null) {
                            int listItemHeight = lastVisibleListItem.getHeight();
                            translationX = getRelativeLeft(thumb, mList) - getRelativeLeft(expandedView, mGrid);
                            int top = thumb.getTop() + listItemHeight * (layoutPos - listItemCount + 1);
                            translationY = getRelativeTop(thumb, mList) - getRelativeTop(expandedView, mGrid) + top;
                        }
                    }

                    final List<View> otherViews = findOtherView(gridItem, mScalingViewId);
                    setVisibility(otherViews, false);

                    Animator animator = getAnimator(toList, gridItem, translationX, translationY, mScaleX, mScaleY, mAnimDuration);
                    animator.addListener(new DefaultAnimListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            setVisibility(otherViews, true);
                        }
                    });
                    anims.add(animator);
                }
            }
        }

        set.playTogether(anims);
        return set;
    }

    @NonNull
    private Animator getAnimator(boolean toList, View target, float translationX,
                                 float translationY, float scaleX, float scaleY, int duration) {

        PropertyValuesHolder valuesHolderX = null;
        PropertyValuesHolder valuesHolderY = null;
        PropertyValuesHolder valuesHolderScaleX = null;
        PropertyValuesHolder valuesHolderScaleY = null;

        if (toList) {
            valuesHolderX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0, translationX);
            valuesHolderY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0, translationY);
            valuesHolderScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1, scaleX);
            valuesHolderScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1, scaleY);
        } else {
            valuesHolderX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, translationX, 0);
            valuesHolderY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, translationY, 0);
            valuesHolderScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, scaleX, 1);
            valuesHolderScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, scaleY, 1);
        }

        target.setPivotX(0);
        target.setPivotY(0);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(target,
                valuesHolderX,
                valuesHolderY,
                valuesHolderScaleX,
                valuesHolderScaleY
        );
        animator.setDuration(duration < 0 ? 0 : duration);
        animator.setInterpolator(new DecelerateInterpolator());

        return animator;
    }

    private int getRelativeLeft(View myView, View rootView) {
        if (myView == null) {
            return 0;
        }

        if (myView.getParent() == rootView) {
            return myView.getLeft();
        }

        return myView.getLeft() + getRelativeLeft((View) myView.getParent(), rootView);
    }

    private int getRelativeTop(View myView, View rootView) {
        if (myView == null) {
            return 0;
        }

        if (myView.getParent() == rootView) {
            return myView.getTop();
        }

        return myView.getTop() + getRelativeTop((View) myView.getParent(), rootView);
    }


    @NonNull
    private List<ThumbItem> filterThumbItems(RecyclerView list) {
        List<ThumbItem> thumbItems = new ArrayList<>();

        LinearLayoutManager lm = (LinearLayoutManager) list.getLayoutManager();
        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        if (first < 0 || last < 0) {
            return thumbItems;
        }
        int listItemCount = last - first + 1;

        View item;
        View thumb;
        for (int i = 0; i < listItemCount; i++) {
            item = list.getChildAt(i);
            thumb = item.findViewById(mThumbViewId);
            if (thumb != null) {
                thumbItems.add(new ThumbItem(item, thumb));
            }
        }

        return thumbItems;
    }

    private class ThumbItem {
        private View item;
        private View thumb;

        public ThumbItem(View item, View thumb) {
            this.item = item;
            this.thumb = thumb;
        }
    }

    public void setAnimDuration(int duration) {
        duration = duration < 0 ? 0 : duration;
        mAnimDuration = duration;
    }

    private List<View> findOtherView(View parent, int exclusiveId) {
        if (parent instanceof ViewGroup) {
            ViewGroup asParent = (ViewGroup) parent;
            ArrayList<View> views = new ArrayList<>(asParent.getChildCount());
            for (int i = 0; i < asParent.getChildCount(); i++) {
                View view = asParent.getChildAt(i);
                if (view.getId() != exclusiveId) {
                    views.add(view);
                }
            }
            return views;
        }else {
            return Collections.EMPTY_LIST;
        }
    }

    private void setVisibility(List<View> views, boolean visible) {
        for (View v : views) {
            v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }
}