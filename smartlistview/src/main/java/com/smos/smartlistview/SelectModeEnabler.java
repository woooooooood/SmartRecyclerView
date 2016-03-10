package com.smos.smartlistview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangbingcong on 16-3-4.
 */
public class SelectModeEnabler {
    private final int mEditableViewId;
    private ModeChangingAnimatorCreator mAnimCreator;
    private RecyclerView mRecyclerView;
    private int mModeChangingDuration = 200;
    private boolean mInEditMode = false;
    private boolean mChangingMode = false;

    public interface ModeChangingAnimatorCreator<V extends View> {
        Animator createModeChangingAnimator(V editableView, boolean editable);
    }

    public SelectModeEnabler(RecyclerView rv, int editableViewId,
                             @NonNull ModeChangingAnimatorCreator animCreator) {
        mRecyclerView = rv;
        mEditableViewId = editableViewId;
        mAnimCreator = animCreator;
    }

    public void changeEditMode(boolean editable) {
        if (!mChangingMode) {
            mChangingMode = true;
            mInEditMode = editable;
            //setItemViewCacheSize(0) can onRemove cached offscreen items, and new created items can
            //still be caches correctly.
            //Without this call, there may be some cached items not passed to adapter's bind method.
            mRecyclerView.setItemViewCacheSize(0);
            getChangeModeAnim(editable, findEditableViews()).start();
        }
    }

    private AnimatorSet getChangeModeAnim(final boolean enterEditMode, @NonNull List<View>
            editableViews) {

        AnimatorSet set = new AnimatorSet();
        set.setDuration(mModeChangingDuration);
        List<Animator> anims = new ArrayList<>(editableViews.size());
        for (View view : editableViews) {
            Animator anim = mAnimCreator.createModeChangingAnimator(view, enterEditMode);
            if (anim != null) {
                anims.add(anim);
            }
        }
        set.playTogether(anims);
        set.addListener(new DefaultAnimListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mChangingMode = false;
            }
        });

        return set;
    }

    @NonNull
    private List<View> findEditableViews() {
        LinearLayoutManager llm = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        int first = llm.findFirstVisibleItemPosition();
        int last = llm.findLastVisibleItemPosition();

        List<View> views = new ArrayList<>();

        for (int i = first; i <= last; i++) {
            RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(i);
            if(vh == null){
                continue;
            }
            View view = vh.itemView;
            View editableView = view.findViewById(mEditableViewId);
            if (editableView != null) {
                views.add(editableView);
            }
        }
        return views;
    }

    public boolean isSelectMode() {
        return mInEditMode;
    }

    public void setModeChangingDuration(int duration) {
        mModeChangingDuration = duration;
    }
    public int getModeChangingInterval(){
        return mModeChangingDuration;
    }
}
