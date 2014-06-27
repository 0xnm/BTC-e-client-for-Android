package com.QuarkLabs.BTCeClient.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.TickersStorage;
import com.QuarkLabs.BTCeClient.models.Ticker;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

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
public class TickersDashboardAdapter extends BaseAdapter implements View.OnClickListener {

    private Context mContext;
    private ArrayList<Ticker> mData = new ArrayList<>();
    private TickersDashboardAdapterCallbackInterface mCallback;
    private int mNumColumns = 0;

    public TickersDashboardAdapter(@NotNull Context context, TickersDashboardAdapterCallbackInterface callback) {
        mContext = context;
        mCallback = callback;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            v = inflater.inflate(R.layout.fragment_home_tickers_dashboard_item, parent, false);
        } else {
            v = convertView;
        }
        Ticker ticker = mData.get(position);
        TextView pair = (TextView) v.findViewById(R.id.tickerPair);
        TextView last = (TextView) v.findViewById(R.id.tickerLastValue);
        TextView buy = (TextView) v.findViewById(R.id.tickerBuyValue);
        TextView sell = (TextView) v.findViewById(R.id.tickerSellValue);
        String pairValue = ticker.getPair().replace("_", "/").toUpperCase(Locale.US);
        pair.setText(pairValue);
        last.setText(String.valueOf(ticker.getLast()));
        buy.setText(String.valueOf(ticker.getBuy()));
        sell.setText(String.valueOf(ticker.getSell()));
        Ticker oldTicker = TickersStorage.loadPreviousData().get(ticker.getPair());
        if (oldTicker != null) {
            last.setTextColor(ticker.getLast() < oldTicker.getLast() ? Color.RED : Color.GREEN);
            buy.setTextColor(ticker.getBuy() < oldTicker.getBuy() ? Color.RED : Color.GREEN);
            sell.setTextColor(ticker.getSell() < oldTicker.getSell() ? Color.RED : Color.GREEN);
        } else {
            last.setTextColor(Color.GREEN);
            buy.setTextColor(Color.GREEN);
            sell.setTextColor(Color.GREEN);
        }
        last.setOnClickListener(this);
        buy.setOnClickListener(this);
        sell.setOnClickListener(this);
        last.setTag(pairValue);
        buy.setTag(pairValue);
        sell.setTag(pairValue);
        return v;
    }

    public void update() {
        mData.clear();
        mData.addAll(TickersStorage.loadLatestData().values());
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        TextView clickedTextView = (TextView) v;
        String pair = (String) v.getTag();
        double price = Double.parseDouble(String.valueOf(clickedTextView.getText()));
        //just a safety measure
        if (mCallback != null) {
            mCallback.onPriceClicked(pair, price);
        }
    }

    public interface TickersDashboardAdapterCallbackInterface {
        public void onPriceClicked(String pair, double price);
    }
}
