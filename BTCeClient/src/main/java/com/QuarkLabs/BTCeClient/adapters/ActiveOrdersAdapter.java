package com.QuarkLabs.BTCeClient.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.DateTimeUtils;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.ActiveOrder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ActiveOrdersAdapter extends BaseAdapter {

    private List<ActiveOrder> activeOrders = Collections.emptyList();

    private final DateFormat dateTimeFormat = DateTimeUtils.createLongDateTimeFormat();

    @Nullable
    private OnCancelOrderClickListener onCancelOrderClickListener;

    @Override
    public int getCount() {
        return activeOrders.size();
    }

    @Override
    public Object getItem(int position) {
        return activeOrders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        ActiveOrder order = (ActiveOrder) getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.fragment_active_orders_item, parent, false);
        } else {
            view = convertView;
        }

        TextView pairView = (TextView) view.findViewById(R.id.ActiveOrderPair);
        TextView typeView = (TextView) view.findViewById(R.id.ActiveOrderType);
        TextView amountView = (TextView) view.findViewById(R.id.ActiveOrderAmount);
        TextView rateView = (TextView) view.findViewById(R.id.ActiveOrderRate);
        TextView timestampView = (TextView) view.findViewById(R.id.ActiveOrderTimestamp);
        TextView orderIdView = (TextView) view.findViewById(R.id.ActiveOrderID);
        ImageView removeButton = (ImageView) view.findViewById(R.id.removeOrder);
        removeButton.setTag(order.getId());
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final long orderId = (long) v.getTag();
                if (onCancelOrderClickListener != null) {
                    onCancelOrderClickListener
                            .onCancelOrderClicked(orderId);
                }
            }
        });

        String pair = order.getPair();
        checkPairFormat(pair);
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
        pairView.setText(PairUtils.serverToLocal(pair));
        typeView.setText(order.getType());
        amountView.setText(order.getAmount()
                + " " + pair.substring(0, 3).toUpperCase(Locale.US));
        rateView.setText(order.getRate()
                + " " + pair.substring(4).toUpperCase(Locale.US));
        calendar.setTimeInMillis(order.getCreatedAt() * 1000L);
        timestampView.setText(dateTimeFormat.format(calendar.getTime()));
        orderIdView.setText(String.valueOf(order.getId()));

        return view;
    }

    private void checkPairFormat(String pair) {
        if (pair == null || pair.length() < 7) {
            throw new IllegalArgumentException("Pair of unknown format: " + pair);
        }
    }

    public void setActiveOrders(@NonNull List<ActiveOrder> activeOrders) {
        Collections.sort(activeOrders, new Comparator<ActiveOrder>() {
            @Override
            public int compare(ActiveOrder lhs, ActiveOrder rhs) {
                return (int) (rhs.getCreatedAt() - lhs.getCreatedAt());
            }
        });
        this.activeOrders = activeOrders;
        notifyDataSetChanged();
    }

    public void removeOrder(long orderId) {
        for (int i = 0; i < activeOrders.size(); i++) {
            if (activeOrders.get(i).getId() == orderId) {
                activeOrders.remove(i);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void setOnCancelOrderClickListener(@Nullable OnCancelOrderClickListener listener) {
        this.onCancelOrderClickListener = listener;
    }

    public interface OnCancelOrderClickListener {
        void onCancelOrderClicked(long orderId);
    }
}
