package com.QuarkLabs.BTCeClient.adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.data.InMemoryStorage;
import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.Ticker;
import com.QuarkLabs.BTCeClient.views.FlippingView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
public class TickersDashboardAdapter extends BaseAdapter implements View.OnClickListener,
        View.OnLongClickListener {

    private final AnimatorSet leftOutAnimation;
    private final AnimatorSet leftInAnimation;
    private final AnimatorSet rightOutAnimation;
    private final AnimatorSet rightInAnimation;
    private final InMemoryStorage inMemoryStorage;
    private final List<Ticker> tickers = new ArrayList<>();

    private final TickersDashboardAdapterCallbackInterface callback;

    private int mNumColumns = 0;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
            DateFormat.SHORT, Locale.getDefault());

    public TickersDashboardAdapter(@NonNull Context context,
                                   TickersDashboardAdapterCallbackInterface callback) {
        this.callback = callback;
        leftOutAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                context, R.animator.card_flip_left_out);
        leftInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                context, R.animator.card_flip_left_in);
        rightOutAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                context, R.animator.card_flip_right_out);
        rightInAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(
                context, R.animator.card_flip_right_in);
        inMemoryStorage = BtcEApplication.get(context).getInMemoryStorage();
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        mNumColumns = numColumns;
    }

    @Override
    public int getCount() {
        return tickers.size();
    }

    @Override
    public Object getItem(int position) {
        return tickers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FlippingView view;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            view = (FlippingView) inflater.inflate(R.layout.fragment_home_tickers_dashboard_item,
                    parent, false);
            view.addAnimators(leftOutAnimation, leftInAnimation,
                    rightOutAnimation, rightInAnimation);
            view.setOnLongClickListener(this);
        } else {
            view = (FlippingView) convertView;
        }
        Ticker ticker = tickers.get(position);
        Ticker oldTicker = inMemoryStorage.getPreviousData().get(ticker.getPair());

        bindFrontSide(view, ticker, oldTicker);
        bindBackSide(view, ticker, oldTicker);

        return view;
    }

    private void bindFrontSide(@NonNull View itemView, @NonNull Ticker ticker,
                               @Nullable Ticker oldTicker) {
        TextView pairFrontView = (TextView) itemView.findViewById(R.id.tickerPairFront);
        TextView lastView = (TextView) itemView.findViewById(R.id.tickerLastValue);
        TextView buyView = (TextView) itemView.findViewById(R.id.tickerBuyValue);
        TextView sellView = (TextView) itemView.findViewById(R.id.tickerSellValue);

        String pairValue = ticker.getPair();

        pairFrontView.setText(pairValue);
        lastView.setText(ticker.getLast().toPlainString());
        buyView.setText(ticker.getBuy().toPlainString());
        sellView.setText(ticker.getSell().toPlainString());

        lastView.setOnClickListener(this);
        buyView.setOnClickListener(this);
        sellView.setOnClickListener(this);
        lastView.setTag(pairValue);
        buyView.setTag(pairValue);
        sellView.setTag(pairValue);
        if (oldTicker != null) {
            lastView.setTextColor(ticker.getLast().compareTo(oldTicker.getLast()) < 0 ?
                    Color.RED : Color.GREEN);
            buyView.setTextColor(ticker.getBuy().compareTo(oldTicker.getBuy()) < 0 ?
                    Color.RED : Color.GREEN);
            sellView.setTextColor(ticker.getSell().compareTo(oldTicker.getSell()) < 0 ?
                    Color.RED : Color.GREEN);
        } else {
            lastView.setTextColor(Color.GREEN);
            buyView.setTextColor(Color.GREEN);
            sellView.setTextColor(Color.GREEN);
        }
    }

    private void bindBackSide(@NonNull View itemView, @NonNull Ticker ticker,
                              @Nullable Ticker oldTicker) {
        TextView highBackView = (TextView) itemView.findViewById(R.id.tickerBackHighValue);
        TextView lowBackView = (TextView) itemView.findViewById(R.id.tickerBackLowValue);
        TextView buyBackView = (TextView) itemView.findViewById(R.id.tickerBackBuyValue);
        TextView sellBackView = (TextView) itemView.findViewById(R.id.tickerBackSellValue);
        TextView updatedBackView = (TextView) itemView.findViewById(R.id.tickerUpdated);
        highBackView.setText(ticker.getHigh().toPlainString());
        lowBackView.setText(ticker.getLow().toPlainString());
        buyBackView.setText(ticker.getBuy().toPlainString());
        sellBackView.setText(ticker.getSell().toPlainString());
        Date updatedDate = new Date(ticker.getUpdated() * 1000);
        updatedBackView.setText(dateFormat.format(updatedDate));
        if (oldTicker != null) {
            highBackView.setTextColor(ticker.getHigh().compareTo(oldTicker.getHigh()) < 0 ?
                    Color.RED : Color.GREEN);
            lowBackView.setTextColor(ticker.getLow().compareTo(oldTicker.getLow()) < 0 ?
                    Color.RED : Color.GREEN);
            buyBackView.setTextColor(ticker.getBuy().compareTo(oldTicker.getBuy()) < 0 ?
                    Color.RED : Color.GREEN);
            sellBackView.setTextColor(ticker.getSell().compareTo(oldTicker.getSell()) < 0 ?
                    Color.RED : Color.GREEN);
        } else {
            highBackView.setTextColor(Color.GREEN);
            lowBackView.setTextColor(Color.GREEN);
            buyBackView.setTextColor(Color.GREEN);
            sellBackView.setTextColor(Color.GREEN);
        }
    }

    public void update(@NonNull Collection<Ticker> newTickers) {
        tickers.clear();
        tickers.addAll(newTickers);
        Collections.sort(tickers, new Comparator<Ticker>() {
            @Override
            public int compare(Ticker lhs, Ticker rhs) {
                return PairUtils.CURRENCY_COMPARATOR.compare(lhs.getPair(), rhs.getPair());
            }
        });
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        TextView clickedTextView = (TextView) v;
        String pair = (String) v.getTag();
        BigDecimal price = new BigDecimal(clickedTextView.getText().toString());
        //just a safety measure
        if (callback != null) {
            callback.onPriceClicked(pair, price);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v instanceof FlippingView) {
            FlippingView flippingView = (FlippingView) v;
            flippingView.startFlipping();
            return true;
        }
        return false;
    }

    public interface TickersDashboardAdapterCallbackInterface {
        void onPriceClicked(String pair, BigDecimal price);
    }
}
