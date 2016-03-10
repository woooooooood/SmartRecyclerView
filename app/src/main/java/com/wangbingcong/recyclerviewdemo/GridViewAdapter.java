package com.wangbingcong.recyclerviewdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wangbingcong.recyclerviewdemo.model.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangbingcong on 16-3-8.
 */
public class GridViewAdapter extends RecyclerView.Adapter<GridViewAdapter.ViewHolder>{

    private static List<ViewModel> list;

    private Context mContext;

    static {
        int count = 50;
        list = new ArrayList<>();
        for(int i=0; i<count; i++){
            list.add(new ViewModel("Item " + i, "description " + i));
        }
    }

    public GridViewAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.gridview_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(list.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView imageView;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.tvDes);
            imageView = (ImageView) itemView.findViewById(R.id.iv);
        }
    }
}
