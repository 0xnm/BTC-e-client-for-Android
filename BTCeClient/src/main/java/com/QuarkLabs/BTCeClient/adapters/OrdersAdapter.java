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

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.QuarkLabs.BTCeClient.ListTypes;
import com.QuarkLabs.BTCeClient.MainActivity;
import com.QuarkLabs.BTCeClient.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class OrdersAdapter extends BaseAdapter {

    private List<JSONObject> mData = new ArrayList<>();
    private Context mContext;
    private LayoutInflater mInflater;
    private ListTypes mListType;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy HH:mm:ss", Locale.US);

    public OrdersAdapter(Context context, ListTypes listType) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListType = listType;
        mDateFormat.setTimeZone(TimeZone.getDefault());
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public JSONObject getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = null;
        TextView timestamp;
        final JSONObject dataToDisplay = getItem(position);
        TextView orderID;
        TextView type;
        TextView amount;
        TextView pair;
        TextView rate;
        switch (mListType) {
            case Transactions:
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.fragment_trans_history_item, parent, false);
                } else {
                    v = convertView;
                }
                TextView description = (TextView) v.findViewById(R.id.TransHistoryDesc);
                timestamp = (TextView) v.findViewById(R.id.TransHistoryTimestamp);
                amount = (TextView) v.findViewById(R.id.TransHistoryAmount);
                try {
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                    description.setText(dataToDisplay.getString("desc"));
                    calendar.setTimeInMillis(Long.parseLong(dataToDisplay.getString("timestamp")) * 1000L);
                    timestamp.setText(mDateFormat.format(calendar.getTime()));
                    amount.setText(dataToDisplay.getString("amount") + dataToDisplay.getString("currency"));


                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case TradeOrders:
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.fragment_trade_history_item, parent, false);
                } else {
                    v = convertView;
                }
                pair = (TextView) v.findViewById(R.id.TradeHistoryPair);
                rate = (TextView) v.findViewById(R.id.TradeHistoryRate);
                amount = (TextView) v.findViewById(R.id.TradeHistoryAmount);
                type = (TextView) v.findViewById(R.id.TradeHistoryType);
                orderID = (TextView) v.findViewById(R.id.TradeHistoryOrderID);
                timestamp = (TextView) v.findViewById(R.id.TradeHistoryTimestamp);

                try {
                    String pairValue = dataToDisplay.getString("pair");
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                    orderID.setText(dataToDisplay.getString("order_id"));
                    calendar.setTimeInMillis(Long.parseLong(dataToDisplay.getString("timestamp")) * 1000L);
                    timestamp.setText(mDateFormat.format(calendar.getTime()));
                    pair.setText(pairValue.replace("_", "/").toUpperCase(Locale.US));
                    rate.setText(dataToDisplay.getString("rate") + " " + pairValue.substring(4).toUpperCase(Locale.US));
                    amount.setText(dataToDisplay.getString("amount") + " " + pairValue.substring(0, 3).toUpperCase(Locale.US));
                    type.setText(dataToDisplay.getString("type"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case ActiveOrders:
                if (convertView == null) {
                    v = mInflater.inflate(R.layout.fragment_active_orders_item, parent, false);
                } else {
                    v = convertView;
                }

                pair = (TextView) v.findViewById(R.id.ActiveOrderPair);
                type = (TextView) v.findViewById(R.id.ActiveOrderType);
                amount = (TextView) v.findViewById(R.id.ActiveOrderAmount);
                rate = (TextView) v.findViewById(R.id.ActiveOrderRate);
                timestamp = (TextView) v.findViewById(R.id.ActiveOrderTimestamp);
                orderID = (TextView) v.findViewById(R.id.ActiveOrderID);
                ImageView remove = (ImageView) v.findViewById(R.id.removeOrder);
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int order_id = dataToDisplay.optInt("id");
                        new AlertDialog.Builder(mContext)
                                .setTitle("Remove confirmation")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new CancelActiveOrder().execute(order_id);
                                    }
                                })
                                .setNegativeButton("No", null)
                                .setMessage("Are you sure you want to delete OrderID="
                                        + dataToDisplay.optString("id") + "?")
                                .show();
                    }
                });

                try {
                    String pairValue = dataToDisplay.getString("pair");
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                    pair.setText(pairValue.replace("_", "/").toUpperCase(Locale.US));
                    type.setText(dataToDisplay.getString("type"));
                    amount.setText(dataToDisplay.getString("amount") + " " + pairValue.substring(0, 3).toUpperCase(Locale.US));
                    rate.setText(dataToDisplay.getString("rate") + " " + pairValue.substring(4).toUpperCase(Locale.US));
                    calendar.setTimeInMillis(Long.parseLong(dataToDisplay.getString("timestamp_created")) * 1000L);
                    timestamp.setText(mDateFormat.format(calendar.getTime()));
                    orderID.setText(String.valueOf(mData.get(position).optInt("id")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            default:
                break;
        }

        return v;
    }

    /**
     * Updates the data in adapter
     *
     * @param jsonObject JSONObject with trades/transactions/active orders
     */
    @SuppressWarnings("unchecked")
    public void updateEntries(JSONObject jsonObject) {
        try {
            List<JSONObject> temp = new ArrayList<>();
            for (Iterator<String> x = jsonObject.getJSONObject("return").keys(); x.hasNext(); ) {
                String key = x.next();

                JSONObject tempObject = new JSONObject();
                tempObject.put("id", key);
                for (Iterator<String> y = jsonObject.getJSONObject("return").getJSONObject(key).keys(); y.hasNext(); ) {
                    String objectKey = y.next();
                    tempObject.put(objectKey, jsonObject.getJSONObject("return").getJSONObject(key).getString(objectKey));
                }

                temp.add(tempObject);
            }

            Collections.sort(temp, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject lhs, JSONObject rhs) {

                    return (int) (rhs.optLong("timestamp") - lhs.optLong("timestamp"));
                }
            });

            mData = temp;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        notifyDataSetChanged();
    }

    /**
     * AsyncTask class to cancel active order
     */
    private class CancelActiveOrder extends AsyncTask<Integer, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Integer... params) {
            int order_id = params[0];
            JSONObject response = null;
            try {
                response = MainActivity.app.cancelOrder(order_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            String text = "";
            try {
                if (jsonObject == null) {
                    text = "Sorry, something went wrong, please try again later";
                } else if (jsonObject.getInt("success") == 0) {
                    text = jsonObject.getJSONObject("error").toString();

                } else {
                    text = "Order was deleted successfully";
                    int order_id = jsonObject
                            .getJSONObject("return")
                            .getInt("order_id");
                    for (int i = 0; i < mData.size(); i++) {
                        if (mData.get(i).getInt("id") == order_id) {
                            mData.remove(i);
                            break;
                        }
                    }
                    notifyDataSetChanged();

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            NotificationCompat.Builder mBuilder = new NotificationCompat
                    .Builder(mContext)
                    .setSmallIcon(R.drawable.ic_stat_bitcoin_sign)
                    .setContentTitle(mContext.getResources().getString(R.string.app_name))
                    .setContentText(text);

            mBuilder.setSound(RingtoneManager
                    .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            NotificationManager mNotificationManager =
                    (NotificationManager) mContext
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(5, mBuilder.build());
        }
    }
}
