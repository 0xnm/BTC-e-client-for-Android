package com.QuarkLabs.BTCeClient.adapters;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.DateTimeUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.Transaction;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class TransactionsAdapter extends BaseAdapter {

    private List<Transaction> transactions = Collections.emptyList();

    private final DateFormat dateTimeFormat = DateTimeUtils.createLongDateTimeFormat();

    public TransactionsAdapter() {
        dateTimeFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public Object getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        Transaction transaction = (Transaction) getItem(position);
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_trans_history_item, parent, false);
        } else {
            view = convertView;
        }
        TextView descriptionView = (TextView) view.findViewById(R.id.TransHistoryDesc);
        TextView timestampView = (TextView) view.findViewById(R.id.TransHistoryTimestamp);
        TextView amountView = (TextView) view.findViewById(R.id.TransHistoryAmount);
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        descriptionView.setText(transaction.getDescription());
        calendar.setTimeInMillis(transaction.getTimestamp() * 1000L);
        timestampView.setText(dateTimeFormat.format(calendar.getTime()));
        amountView.setText(transaction.getAmount().toPlainString()
                + " " + transaction.getCurrency());
        return view;
    }

    public void setTransactions(@NonNull List<Transaction> transactions) {
        Collections.sort(transactions, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction lhs, Transaction rhs) {
                return (int) (rhs.getTimestamp() - lhs.getTimestamp());
            }
        });
        this.transactions = transactions;
        notifyDataSetChanged();
    }
}
