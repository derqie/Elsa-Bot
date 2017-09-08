package com.android.elsabot.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.elsabot.R;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by Derrick on 9/6/2017.
 */
public class ListAdapter extends ArrayAdapter<ListObject> {

    private List<ListObject> array;
    private Context context;


    public ListAdapter(Context lContext,List<ListObject> lArray) {
        super(lContext, 0);

        context = lContext;
        array = lArray;
    }

    @Override
    public View getView(int position, View contentView, ViewGroup parent) {
        ViewHolder holder;

        if (contentView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            contentView = inflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder(contentView);
            contentView.setTag(holder);
        } else {
            holder = (ViewHolder) contentView.getTag();
        }

        ListObject spot = getItem(position);

        holder.query.setText(spot.query);
        holder.result.setText(spot.result);


        if(holder.result.getText().toString().equals("")){
            holder.result.setText("Question was not clear!");
        }

        //if(holder.result.getText().toString().equals("")){
            //holder.result.setVisibility(View.GONE);
            //holder.query.setVisibility(View.VISIBLE);

        //}if(holder.query.getText().toString().equals("")){
            //holder.result.setVisibility(View.VISIBLE);
            //holder.query.setVisibility(View.GONE);
        //}


        return contentView;
    }

    private static class ViewHolder {
        public TextView query,result;

        public ViewHolder(View view) {
            this.query = (TextView) view.findViewById(R.id.query);
            this.result = (TextView) view.findViewById(R.id.result);

        }
    }

}
