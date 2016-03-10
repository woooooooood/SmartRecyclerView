package com.wangbingcong.recyclerviewdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smos.smartlistview.SectionItemArray;
import com.smos.smartlistview.SelectItemListener;
import com.smos.smartlistview.SmartListViewManager;
import com.smos.smartlistview.StickyHeaderDecoration;
import com.smos.smartlistview.SwipeItemListener;

/**
 * Created by wangbingcong on 16-2-25.
 */
public class SwipeMenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements SwipeItemListener.SwipeMenuBinder<SwipeMenuAdapter.MySwipeMenu>,
        SelectItemListener.OnSelectListener, StickyHeaderDecoration.HeaderProvider {

    private final RecyclerView mRecyclerView;
    private SmartListViewManager mManager;
    private Context mContext;
    private MainActivity mActivity;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_SECTION = 0;
    private SectionItemArray<String> mItemArray;


    public SwipeMenuAdapter(Context context, SmartListViewManager manager, RecyclerView
            recyclerView, SectionItemArray sectionItemArray) {
        mContext = context;
        if (mContext instanceof MainActivity) {
            mActivity = (MainActivity) mContext;
        }
        this.mManager = manager;
        mItemArray = sectionItemArray;
        mRecyclerView = recyclerView;
    }

    @Override
    public int getItemCount() {
        return mItemArray.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mItemArray.isSection(position)) {
            return VIEW_TYPE_SECTION;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SECTION) {
            return createSectionViewHolder(parent);
        } else {
            return createItemViewHolder(parent);
        }
    }

    private RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }

    private RecyclerView.ViewHolder createSectionViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.smartlist_header, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (mItemArray.isSection(position)) {
            bindSectionViewHolder(holder, position);
        } else {
            bindItemViewHolder(holder, position);
        }
    }

    private void bindSectionViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        SectionViewHolder holder = (SectionViewHolder) viewHolder;
        String section = mItemArray.getSection(position);
        holder.textView.setText(section);
    }

    private void bindItemViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        ViewHolder holder = (ViewHolder) viewHolder;
        String item = mItemArray.get(position);
        holder.textView.setText(item);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext, position + "clicked", Toast.LENGTH_SHORT).show();
            }
        });
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemArray.contains(position)) {
                    mItemArray.deSelect(position);
                    ((CheckBox) v).setChecked(false);
                } else {
                    mItemArray.select(position);
                    ((CheckBox) v).setChecked(true);
                }
            }
        });
        if (mItemArray.contains(position)) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }

        if (mActivity != null && mActivity.mSelectModeEnabler != null) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.editableView
                    .getLayoutParams();
            if (mActivity.mSelectModeEnabler.isSelectMode()) {
                params.leftMargin = 0;
            } else {
                params.leftMargin = (int) mContext.getResources()
                        .getDimension(R.dimen.editableview_margin_left);
            }
        }
    }

    public void setManager(SmartListViewManager manager) {
        this.mManager = manager;
    }

    public void deleteSelectedData() {
        mManager.removeItems(mItemArray.getSelectedItem());
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public SectionViewHolder(View headerView) {
            super(headerView);
            textView = (TextView) headerView.findViewById(R.id.text);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        CheckBox checkBox;

        LinearLayout editableView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.tv);
            editableView = (LinearLayout) itemView.findViewById(R.id.editableView);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
        }
    }

    @Override
    public MySwipeMenu onCreateSwipeMenuView(ViewGroup parent, int menuType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.swipemenu_layout, parent, false);
        return new MySwipeMenu(view, menuType);
    }

    @Override
    public int getSwipeMenuType(int position) {
        return 0;
    }

    @Override
    public void onBindSwipeMenu(final RecyclerView.ViewHolder holder, MySwipeMenu swipeMenu,
                                final int position) {
        swipeMenu.btn.setText(((ViewHolder) holder).textView.getText() + "");
        swipeMenu.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemArray.select(position);
                mManager.removeItems(mItemArray.getSelectedItem());
            }
        });
    }

    @Override
    public boolean canItemSwipe(int position) {
        return !isHeader(position);
    }

    public static class MySwipeMenu extends SwipeItemListener.SwipeMenu {
        Button btn;

        public MySwipeMenu(View swipeMenuView, int menuType) {
            super(swipeMenuView, menuType);
            btn = (Button) swipeMenuView.findViewById(R.id.btnToast);
        }
    }

    @Override
    public void onSelect(int position, boolean flag) {
        if (!mItemArray.isSection(position)) {
            View child = mRecyclerView.findViewHolderForAdapterPosition(position).itemView;
            CheckBox checkBox = (CheckBox) child.findViewById(R.id.checkbox);
            if (flag) {
                mItemArray.select(position);
                checkBox.setChecked(true);
            } else {
                mItemArray.deSelect(position);
                checkBox.setChecked(false);
            }
        }
    }

    @Override
    public boolean isSelected(int position) {
        return mItemArray.contains(position);
    }

    @Override
    public View getHeader(int position) {
        String section = mItemArray.getSection(position);
        if (section != null) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.smartlist_header, null);
            TextView textView = (TextView) view.findViewById(R.id.text);
            textView.setText(section);
            return view;
        }
        return null;
    }

    @Override
    public boolean isHeader(int position) {
        return mItemArray.isSection(position);
    }
}
