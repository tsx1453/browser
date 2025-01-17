package com.liuzho.browser.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.liuzho.browser.R;

import java.util.ArrayList;
import java.util.List;

public class GridAdapter extends BaseAdapter {
    private static class Holder {
        TextView title;
        ImageView icon;
    }

    private final List<GridItem> list = new ArrayList<>();

    private final Context context;

    public GridAdapter(Context context, List<GridItem> list) {
        this.context = context;
        this.list.addAll(list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.libbrs_item_icon_left, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.record_item_title);
            holder.icon = view.findViewById(R.id.record_item_icon);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        GridItem item = list.get(position);
        holder.title.setText(item.getTitle());
        holder.icon.setImageResource(item.getIcon());
        if (item.getIcon() != 0) holder.icon.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int arg0) {
        return list.get(arg0);

    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    public void refresh(List<GridItem> newList) {
        list.clear();
        list.addAll(newList);
        notifyDataSetChanged();
    }
}
