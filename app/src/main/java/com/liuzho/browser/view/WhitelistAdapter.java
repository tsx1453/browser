package com.liuzho.browser.view;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import com.liuzho.browser.browser.TrustedList;
import com.liuzho.browser.R;

public class WhitelistAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final int layoutResId;
    private final List<String> list;

    public WhitelistAdapter(Context context, List<String> list){
        super(context, R.layout.libbrs_item_icon_right, list);
        this.context = context;
        this.layoutResId = R.layout.libbrs_item_icon_right;
        this.list = list;
    }

    private static class Holder {
        TextView domain;
        ImageButton cancel;
    }

    @SuppressWarnings("NullableProblems")
    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        Holder holder;
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
            holder = new Holder();
            holder.domain = view.findViewById(R.id.whitelist_item_domain);
            holder.cancel = view.findViewById(R.id.whitelist_item_cancel);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        holder.domain.setText(list.get(position));
        holder.cancel.setOnClickListener(v -> {
            TrustedList Javascript = new TrustedList(context);
            Javascript.removeDomain(list.get(position));
            list.remove(position);
            notifyDataSetChanged();
            NinjaToast.show(context, R.string.libbrs_toast_delete_successful);
        });

        return view;
    }
}
