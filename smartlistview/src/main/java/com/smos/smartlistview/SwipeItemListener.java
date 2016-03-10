package com.smos.smartlistview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import java.util.HashMap;

/**
 * Created by wangbingcong on 16-2-23.
 */

/**
 * Only one SwipeMenu can be opened at a time.
 */
public class SwipeItemListener implements RecyclerView.OnItemTouchListener {

    private final String TAG = "ItemSwipeMenuEnabler";
    private final int DEFAULT_ANIM_DURATION = 100;
    private final Interpolator DEFAULT_INTERPOLATOR = new OvershootInterpolator(0.75f);
    private final float INITIAL_TOUCH_X_NONE = -1;
    private RecyclerView mRecyclerView;
    private Context mContext;
    /**
     * Used to create and config SwipeMenu.
     */
    private SwipeMenuBinder mSwipeMenuBinder;
    /**
     * Registered Listener from out side. Changes of SwipeMenu state will be notified.
     */
    private SwipeMenuStateListener mSwipeMenuStateListener;
    /**
     * Element views Will be shown as SwipeMenu while swiping according to the given menu type.
     * key : menu type;
     * value : typed SwipeMenu.
     */
    private HashMap<Integer, SwipeMenu> mReusedSwipeMenus;
    /**
     * SwipeMenu to display
     */
    private SwipeMenu mSwipeMenu;
    /**
     * Focused ViewHolder.
     */
    private SwipableViewHolder mFocusedVH;
    /**
     * indicates whether the SwipeMenu opening/closing animation is performing
     */
    private boolean mAnimating;
    private int mRecoverAnimDuration = DEFAULT_ANIM_DURATION;
    private Interpolator mRecoverAnimInterpolator = DEFAULT_INTERPOLATOR;
    /**
     * Indicates whether there is an opened SwipeMenu, for we will close the opened SwipeMenu
     * at next touch event if there is.
     */
    private boolean mHasSwipeMenuOpened;
    private int mThresholdXtoOpenSwipeMenu = 30;
    private int mTouchSlop;
    private float mLastX;
    /**
     * X on touch down
     */
    private float mInitialTouchX;

    private boolean mEnable = true;


    public SwipeItemListener(RecyclerView recyclerView, SwipeMenuBinder menuBinder) {
        mRecyclerView = recyclerView;
        mSwipeMenuBinder = menuBinder;
        init(recyclerView.getContext());
    }

    private void init(Context context) {
        mContext = context;
        mReusedSwipeMenus = new HashMap<>();
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mTouchSlop = vc.getScaledTouchSlop();
        mAnimating = false;
        mHasSwipeMenuOpened = false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (!mEnable) {
            return false;
        }
        if (mHasSwipeMenuOpened) {
            boolean hit = hitSwipeMenu(e);
            if (hit && rv.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                closeOpeningMenu();
                return true;
            }
            return !hit;
        }
        if (rv.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            // should not open SwipeMenu while scrolling
            return false;
        }
        if (mAnimating) {
            // intercept to disable scrolling while animating or dragging
            return true;
        }
        // select vh
        selectIfNeeded(rv, e);
        return mFocusedVH != null;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        if (mFocusedVH == null) {
            return;
        }

        if (mAnimating) {
            return;
        }

        if (mHasSwipeMenuOpened) {
            closeOpeningMenu();
            return;
        }

        int x = (int) e.getX();
        switch (e.getAction()) {
            // notice that ACTION_DOWN won't be dispatched to here
            case MotionEvent.ACTION_UP:
                recoverItemView();
                break;

            case MotionEvent.ACTION_MOVE:
                mSwipeMenu = getTypedSwipeMenu();

                float dx = x - mLastX;
                View topView = mFocusedVH.getTopView();
                float overSwipe = topView.getTranslationX() - mSwipeMenu.itemSettleLeft;
                if (overSwipe > 0 && dx > 0) {
                    dx = dx / (1 + dx * dx * (overSwipe / mSwipeMenu.itemSettleLeft));
                }
                float translateX = topView.getTranslationX() + dx;
                topView.setTranslationX(translateX <= 0 ? 0 : translateX);
                break;
        }

        mLastX = e.getX();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        //TODO onRequestDisallowInterceptTouchEvent
    }

    private void selectIfNeeded(RecyclerView rv, MotionEvent e) {
        float x = e.getX();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mFocusedVH == null) {
                    mInitialTouchX = x;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mFocusedVH == null && mInitialTouchX != INITIAL_TOUCH_X_NONE) {
                    if (x - mInitialTouchX > mTouchSlop) {
                        View itemView = rv.findChildViewUnder(x, e.getY());
                        if (itemView == null) {
                            return;
                        }

                        mFocusedVH = (SwipableViewHolder) rv.getChildViewHolder(itemView);

                        if (!mSwipeMenuBinder.canItemSwipe(mFocusedVH.getAdapterPosition())) {
                            mFocusedVH = null;
                            return;
                        }

                        mSwipeMenu = getTypedSwipeMenu();
                        if (mSwipeMenu != null) {
                            mSwipeMenuBinder.onBindSwipeMenu(mFocusedVH.holder, mSwipeMenu,
                                    mFocusedVH.getAdapterPosition());
                            mFocusedVH.replaceMenu(mSwipeMenu);
                        }

                        mLastX = e.getX();
                    }
                }
                break;
        }
    }

    private void deselect() {
        mFocusedVH = null;
        mHasSwipeMenuOpened = false;
        mInitialTouchX = INITIAL_TOUCH_X_NONE;
    }

    public void setEnable(boolean enable) {
        mEnable = enable;
        if (!enable && mHasSwipeMenuOpened) {
            closeOpeningMenu();
        }
    }

    public boolean isEnabled() {
        return mEnable;
    }

    public synchronized void closeOpeningMenu() {
        if (mHasSwipeMenuOpened) {
            getSwipeAnimator(mFocusedVH.getTopView(), 0).start();
        }
    }

    public synchronized void openItemMenu(final int position) {
        if (mHasSwipeMenuOpened) {
            if (position == mFocusedVH.getAdapterPosition()) {
                return;
            }
            Animator animator = getSwipeAnimator(mFocusedVH.getTopView(), 0);
            Animator.AnimatorListener listener = new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    doOpenItemMenu(position);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            };
            animator.addListener(listener);
            animator.start();
        } else {
            doOpenItemMenu(position);
        }
    }

    private synchronized void doOpenItemMenu(int position) {
        mFocusedVH = getSwipableViewHolder(position);
        if (mFocusedVH == null) {
            return;
        }
        mSwipeMenu = getTypedSwipeMenu();
        mSwipeMenuBinder.onBindSwipeMenu(mFocusedVH.holder, mSwipeMenu, mFocusedVH
                .getAdapterPosition());
        mFocusedVH.replaceMenu(mSwipeMenu);
        getSwipeAnimator(mFocusedVH.getTopView(), mSwipeMenu.itemSettleLeft).start();
    }

    private SwipeMenu getTypedSwipeMenu() {
        int position = mFocusedVH.getAdapterPosition();
        int menuType = mSwipeMenuBinder.getSwipeMenuType(position);
        SwipeMenu swipeMenu = mReusedSwipeMenus.get(menuType);
        if (swipeMenu == null) {
            ViewGroup parent = mFocusedVH.swipableItem.getRootView();
            swipeMenu = mSwipeMenuBinder.onCreateSwipeMenuView(parent, menuType);
            swipeMenu.view = getInflatedViewRoot(parent, swipeMenu.view);
            mReusedSwipeMenus.put(menuType, swipeMenu);
        }
        return swipeMenu;
    }

    private SwipableViewHolder getSwipableViewHolder(int position) {
        return (SwipableViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
    }

    private boolean hitSwipeMenu(MotionEvent e) {
        boolean hit = false;
        SwipeMenu swipeMenu = getTypedSwipeMenu();
        View itemView = mFocusedVH.swipableItem.getRootView();
        float l = itemView.getLeft();
        float t = itemView.getTop();
        float r = swipeMenu.itemSettleLeft + l;
        float b = itemView.getBottom();
        float x = e.getX();
        float y = e.getY();
        if (x <= r && x >= l && y >= t && y <= b) {
            hit = true;
        }

        return hit;
    }

    private void recoverItemView() {
        if (mFocusedVH == null) {
            return;
        }

        float translationX;
        float topViewLeft = mFocusedVH.getTopView().getTranslationX();
        if (topViewLeft > mThresholdXtoOpenSwipeMenu) {
            // opening
            translationX = mSwipeMenu.itemSettleLeft;
        } else {
            // closing
            translationX = 0;
        }

        getSwipeAnimator(mFocusedVH.getTopView(), translationX).start();
    }

    private Animator getSwipeAnimator(Object target, float value) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "translationX", value)
                .setDuration(mRecoverAnimDuration);

        animator.setInterpolator(mRecoverAnimInterpolator);

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
                mHasSwipeMenuOpened = mFocusedVH.getTopView().getTranslationX() >
                        mThresholdXtoOpenSwipeMenu;

                if (mSwipeMenuStateListener != null) {
                    int state = mHasSwipeMenuOpened ? SwipeMenuStateListener.STATE_OPENED
                            : SwipeMenuStateListener.STATE_CLOSED;
                    mSwipeMenuStateListener.onSwipeStateChanged(state,
                            mFocusedVH.getAdapterPosition());
                }

                if (!mHasSwipeMenuOpened) {
                    deselect();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        return animator;
    }

    public void setRecoverAnimDuration(int duration) {
        if (duration < 0) {
            duration = 0;
        }
        mRecoverAnimDuration = duration;
    }

    public void setRecoverAnimInterpolator(@Nullable Interpolator interpolator) {
        if (interpolator == null) {
            return;
        }
        mRecoverAnimInterpolator = interpolator;
    }

    public void setSwipeMenuStateListener(SwipeMenuStateListener swipeMenuStateListener) {
        mSwipeMenuStateListener = swipeMenuStateListener;
    }

    public interface SwipeMenuStateListener {
        int STATE_OPENED = 0;
        int STATE_CLOSED = 1;

        void onSwipeStateChanged(int swipeState, int position);
    }

    /**
     * RecycleViews who want to enable SwipeMenu must have their adapters implement this interface
     * to configure their specific SwipeMenus, or a exception would be thrown.
     */
    public interface SwipeMenuBinder<SM extends SwipeMenu> {
        /**
         * To create typed SwipeMenu for specific menuType.
         * This method will only be called when the menu-typed SwipeMenu does not exist.
         *
         * @return A view holder of typed SwipeMenu according to given menuType.
         */
        SM onCreateSwipeMenuView(ViewGroup parent, int menuType);

        int getSwipeMenuType(int position);

        void onBindSwipeMenu(RecyclerView.ViewHolder holder, SM swipeMenu, int position);

        boolean canItemSwipe(int position);
    }

    /**
     * This Decorator wraps the adapter of the attached RecyclerView.
     * It will be set to the RecyclerView as adapter instead.
     * Each view created by the original adapter will be added to a container as a top view.
     * This container, composing a bottom view acts as SwipeMenu, will be set to the RecyclerView
     * as real itemView.
     * <p/>
     * Clients of ItemSwipeMenuEnabler won't notice about the existence of SwipableAdapterDecorator.
     */
    public static class SwipableAdapterDecorator extends RecyclerView.Adapter<SwipableViewHolder> {
        private RecyclerView.Adapter mAdapter;
        private Context mContext;
        private RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver
                () {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }
        };

        public SwipableAdapterDecorator(Context context, RecyclerView.Adapter adapter) {
            if (adapter == null) {
                throw new IllegalArgumentException("Illegal adapter is null.");
            }
            mContext = context;
            mAdapter = adapter;
            mAdapter.registerAdapterDataObserver(mObserver);
        }

        @Override
        public SwipableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            SwipableItem swipableItem = new SwipableItem(mContext, null, null);
            parent.addView(swipableItem.getRootView());
            RecyclerView.ViewHolder vh = mAdapter.onCreateViewHolder(swipableItem.getRootView(),
                    viewType);
            if (vh.itemView.getParent() != null) {
                throw new IllegalStateException("The specified child already has a parent. " +
                        "You must call removeView() on the child's parent first.");
            }
            swipableItem.setTopView(vh.itemView);
            return new SwipableViewHolder(swipableItem, vh);
        }

        @Override
        public void onBindViewHolder(SwipableViewHolder holder, int position) {
            ViewHelper.clear(holder.getTopView());
            mAdapter.onBindViewHolder(holder.holder, position);
        }

        @Override
        public int getItemCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            return mAdapter.getItemViewType(position);
        }

        @Override
        public long getItemId(int position) {
            return mAdapter.getItemId(position);
        }
    }

    /**
     * SwipableViewHolder wraps the ViewHolder created by the adapter.
     */
    private static class SwipableViewHolder extends RecyclerView.ViewHolder {
        private SwipableItem swipableItem;
        private RecyclerView.ViewHolder holder;

        public SwipableViewHolder(SwipableItem swipableItem, RecyclerView.ViewHolder holder) {
            super(swipableItem.getRootView());
            this.swipableItem = swipableItem;
            this.holder = holder;
        }

        void replaceMenu(SwipeMenu swipeMenu) {
            View menu = swipeMenu.view;

            if (menu != null && menu.getParent() != null) {
                ViewGroup p = (ViewGroup) menu.getParent();
                p.removeView(menu);
            }

            swipableItem.setBottomView(menu);
            // topView settles at menu width
            View itemRoot = swipableItem.getRootView();
            itemRoot.measure(View.MeasureSpec.makeMeasureSpec(itemRoot.getWidth(), View
                            .MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(itemRoot.getHeight(), View.MeasureSpec
                            .EXACTLY));
            swipeMenu.itemSettleLeft = menu.getMeasuredWidth();
        }

        View getTopView() {
            return swipableItem.getTopView();
        }

        View getBottomView() {
            return swipableItem.getBottomView();
        }
    }

    /**
     * A class represents SwipableItem in the enhanced RecyclerView.
     * Each item consists of a bottomView, acts as swipeMenu, and a topView.
     * A pair of top and bottom views are contained in a FrameLayout, which will be set to the
     * RecyclerView as an item view.
     */
    private static class SwipableItem {
        View mTopView;
        View mBottomView;
        FrameLayout mContainer;

        SwipableItem(Context context, View topView, View bottomView) {
            mContainer = new FrameLayout(context);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams
                    .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mContainer.setLayoutParams(params);

            setTopView(topView);
            setBottomView(bottomView);
        }

        ViewGroup getRootView() {
            return mContainer;
        }

        public View getTopView() {
            return mTopView;
        }

        void setTopView(@Nullable View topView) {
            if (mTopView != null) {
                mContainer.removeView(mTopView);
                mTopView = null;
            }
            if (topView != null) {
                mContainer.addView(topView);
                mTopView = topView;
            }
        }

        public View getBottomView() {
            return mBottomView;
        }

        void setBottomView(@Nullable View bottomView) {
            if (mBottomView != null) {
                mContainer.removeView(mBottomView);
                mBottomView = null;
            }

            if (bottomView != null) {
                mContainer.addView(bottomView, 0);
                mBottomView = bottomView;
            }
        }
    }

    /**
     * A class represents SwipeMenu displayed beneath each Item
     */
    public static abstract class SwipeMenu {
        // the real view to display as Menu
        View view;
        // x distance that the item can be swiped
        private float itemSettleLeft = 0;
        int menuType;

        public SwipeMenu(View swipeMenuView, int menuType) {
            view = swipeMenuView;
            this.menuType = menuType;
        }
    }

    //FIXME
    private static View getInflatedViewRoot(ViewGroup parent, View inflated) {
        if (parent == inflated) {
            View root;
            int index = parent.getChildCount() - 1;
            if (index < 0) {
                return null;
            }
            root = parent.getChildAt(index);
            parent.removeView(root);
            return root;
        } else {
            return inflated;
        }
    }

}