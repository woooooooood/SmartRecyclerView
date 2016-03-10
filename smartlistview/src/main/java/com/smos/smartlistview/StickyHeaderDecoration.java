package com.smos.smartlistview;

import android.graphics.Canvas;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by zhangjunxing on 16-3-7.
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    HeaderProvider mHeaderProvider;

    public StickyHeaderDecoration(HeaderProvider headerProvider) {
        mHeaderProvider = headerProvider;
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(canvas, parent, state);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(canvas, parent, state);
        View currentHeader = getCurrentHeader(parent);
        if (currentHeader == null) {
            return;
        }
        measureHeader(parent, currentHeader);
        float headerOffset = configureHeader(parent, currentHeader);
        int saveCount = canvas.save();
        canvas.translate(0, headerOffset);
        canvas.clipRect(0, 0, parent.getWidth(), currentHeader.getMeasuredHeight()); // needed
        // for
        // <
        // HONEYCOMB
        currentHeader.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    private float configureHeader(RecyclerView recyclerView, View currentHeader) {
        float headerOffset = 0.0f;
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View header = recyclerView.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) header.getLayoutParams();
            if (!params.isViewInvalid()) {
                RecyclerView.ViewHolder childViewHolder = recyclerView.getChildViewHolder(header);
                int adapterPosition = childViewHolder.getAdapterPosition();
                if (mHeaderProvider.isHeader(adapterPosition)) {
                    float headerTop = header.getTop();
                    float pinnedHeaderHeight = currentHeader.getMeasuredHeight();
                    if (pinnedHeaderHeight >= headerTop && headerTop > 0) {
                        headerOffset = headerTop - header.getHeight();
                    }
                }
            }
        }
        return headerOffset;
    }

    private void measureHeader(RecyclerView parent, View header) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View
                .MeasureSpec.EXACTLY);
        int heightMeasureSpec;

        ViewGroup.LayoutParams params = header.getLayoutParams();
        if (params != null && params.height > 0) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(params.height, View.MeasureSpec
                    .EXACTLY);
        } else {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        }
        header.measure(widthMeasureSpec, heightMeasureSpec);
        header.layout(parent.getLeft() + parent.getPaddingLeft(), 0, parent.getRight() - parent
                        .getPaddingRight(),
                header.getMeasuredHeight());
    }

    private View getCurrentHeader(RecyclerView recyclerView) {
        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisiblePos = lm.findFirstVisibleItemPosition();
        return mHeaderProvider.getHeader(firstVisiblePos);
    }

    public interface HeaderProvider {
        View getHeader(int position);

        boolean isHeader(int position);
    }
}
