/*
 * BTC-e client
 *     Copyright (C) 2014  QuarkDev Solutions <quarkdev.solutions@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.QuarkLabs.BTCeClient.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.QuarkLabs.BTCeClient.R;
import org.json.JSONArray;

public class OrdersBookAdapter extends BaseAdapter {

    private JSONArray mData;
    private LayoutInflater mInflater;
    private double mMaxValue = 0;

    public OrdersBookAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mData.length();
    }

    @Override
    public Object getItem(int position) {
        return mData.opt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = mInflater.inflate(R.layout.ordersbook_item, parent, false);
        } else {
            v = convertView;
        }
        JSONArray value = mData.optJSONArray(position);
        double price = value.optDouble(0);
        double volume = value.optDouble(1);
        TextView text1 = (TextView) v.findViewById(R.id.orderBookPrice);
        TextView text2 = (TextView) v.findViewById(R.id.ordersBookVolume);
        text1.setText(String.valueOf(price));
        text2.setText(String.valueOf(volume));
        if (volume == mMaxValue) {
            text1.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            text2.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            text1.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            text2.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }

        return v;
    }

    /**
     * Updates data in adapter
     *
     * @param data JSONArray with price-volume pairs
     */
    public void pushData(JSONArray data) {
        mData = data;
        for (int i = 0; i < data.length(); i++) {
            mMaxValue = mMaxValue < data.optJSONArray(i).optDouble(1) ? data.optJSONArray(i).optDouble(1) : mMaxValue;
        }
        notifyDataSetChanged();
    }
}
