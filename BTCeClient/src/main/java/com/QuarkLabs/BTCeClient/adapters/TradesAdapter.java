package com.QuarkLabs.BTCeClient.adapters;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.DateTimeUtils;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.TradeHistoryEntry;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TradesAdapter extends BaseAdapter {

    private List<TradeHistoryEntry> trades;

    private DateFormat dateTimeFormat = DateTimeUtils.createLongDateTimeFormat();

    public TradesAdapter() {
        dateTimeFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    public int getCount() {
        return trades.size();
    }

    @Override
    public Object getItem(int position) {
        return trades.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TradeHistoryEntry trade = (TradeHistoryEntry) getItem(position);
        View v;
        if (convertView == null) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_trade_history_item, parent, false);
        } else {
            v = convertView;
        }

        TextView pairView = (TextView) v.findViewById(R.id.TradeHistoryPair);
        TextView rateView = (TextView) v.findViewById(R.id.TradeHistoryRate);
        TextView amountView = (TextView) v.findViewById(R.id.TradeHistoryAmount);
        TextView typeView = (TextView) v.findViewById(R.id.TradeHistoryType);
        TextView orderIdView = (TextView) v.findViewById(R.id.TradeHistoryOrderID);
        TextView timestampView = (TextView) v.findViewById(R.id.TradeHistoryTimestamp);

        String pairValue = trade.getPair();
        checkPairFormat(pairValue);

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        orderIdView.setText(String.valueOf(trade.getOrderId()));
        calendar.setTimeInMillis(trade.getTimestamp() * 1000L);
        timestampView.setText(dateTimeFormat.format(calendar.getTime()));
        pairView.setText(PairUtils.serverToLocal(pairValue));
        rateView.setText(trade.getRate() + " "
                + pairValue.substring(4).toUpperCase(Locale.US));
        amountView.setText(trade.getAmount() + " "
                + pairValue.substring(0, 3).toUpperCase(Locale.US));
        typeView.setText(String.valueOf(trade.getType()));

        return v;
    }

    private void checkPairFormat(String pair) {
        if (pair == null || pair.length() < 7) {
            throw new IllegalArgumentException("Pair of unknown format: " + pair);
        }
    }

    public void setTrades(@NonNull List<TradeHistoryEntry> trades) {
        Collections.sort(trades, new Comparator<TradeHistoryEntry>() {
            @Override
            public int compare(TradeHistoryEntry lhs, TradeHistoryEntry rhs) {
                return (int) (rhs.getTimestamp() - lhs.getTimestamp());
            }
        });
        this.trades = trades;
        notifyDataSetChanged();
    }
}
