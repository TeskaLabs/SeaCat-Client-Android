package com.teskalabs.seacat.android.companion.Base;


import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;

import com.teskalabs.seacat.android.companion.R;


public class DrawerNavAdapter extends ArrayAdapter<DrawerNavItem> {
    Context mContext;
    int layoutResourceId;
    DrawerNavItem data[] = null;

    public DrawerNavAdapter(Context mContext, int layoutResourceId, DrawerNavItem[] data) {

        super(mContext, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listItem = convertView;

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        listItem = inflater.inflate(layoutResourceId, parent, false);

        ImageView imageViewIcon = (ImageView) listItem.findViewById(R.id.imageViewIcon);
        TextView textViewName = (TextView) listItem.findViewById(R.id.textViewName);

        if (data[position].icon != -1)
            imageViewIcon.setImageResource(data[position].icon);
        textViewName.setText(data[position].name);

        return listItem;
    }
}
