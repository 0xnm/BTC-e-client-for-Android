/*
 * WEX client
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

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.PriceVolumePair;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class OrdersBookAdapter extends BaseAdapter {

    private List<PriceVolumePair> data;
    private double maxVolume = 0;

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ordersbook_item, parent, false);
        } else {
            v = convertView;
        }
        PriceVolumePair priceVolume = (PriceVolumePair) getItem(position);
        double price = priceVolume.getPrice();
        double volume = priceVolume.getVolume();
        TextView priceView = (TextView) v.findViewById(R.id.orderBookPrice);
        TextView volumeView = (TextView) v.findViewById(R.id.ordersBookVolume);
        priceView.setText(decimalToStringWithoutExponent(price));
        volumeView.setText(decimalToStringWithoutExponent(volume));
        if (volume == maxVolume) {
            priceView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            volumeView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            priceView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            volumeView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }

        return v;
    }

    private String decimalToStringWithoutExponent(double value) {
        DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setMaximumFractionDigits(340);
        return df.format(value);
    }

    /**
     * Updates data in adapter
     *
     * @param data List of {@link PriceVolumePair}
     */
    public void pushData(List<PriceVolumePair> data) {
        this.data = data;
        for (int i = 0; i < data.size(); i++) {
            maxVolume = maxVolume < data.get(i).getVolume() ?
                    data.get(i).getVolume() : maxVolume;
        }
        notifyDataSetChanged();
    }
}
