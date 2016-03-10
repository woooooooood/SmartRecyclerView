package com.smos.smartlistview;

import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

/**
 * Created by wangbingcong on 16-3-10.
 */
public class TouchEventInterceptor implements RecyclerView.OnItemTouchListener {
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return true;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
