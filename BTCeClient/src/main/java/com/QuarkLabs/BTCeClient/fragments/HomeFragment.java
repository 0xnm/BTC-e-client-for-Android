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

package com.QuarkLabs.BTCeClient.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.*;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.QuarkLabs.BTCeClient.*;
import com.QuarkLabs.BTCeClient.adapters.CheckBoxListAdapter;
import com.QuarkLabs.BTCeClient.adapters.TickersDashboardAdapter;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;
import com.QuarkLabs.BTCeClient.models.Ticker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class HomeFragment extends Fragment implements TickersDashboardAdapter.TickersDashboardAdapterCallbackInterface {

    private static final double DOUBLEFEE = 0.004;
    private FixedGridView mTickersContainer;
    private TickersDashboardAdapter mTickersDashboardAdapter;
    private BroadcastReceiver mGetStatsReceiver;
    private ActivityCallbacks mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (ActivityCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ActivityCallbacks");
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mTickersContainer = (FixedGridView) getView().findViewById(R.id.tickersContainer);
        mTickersContainer.setExpanded(true);
        final int dashboardSpacing = getResources().getDimensionPixelSize(R.dimen.dashboard_spacing);
        final int dahboardItemSize = getResources().getDimensionPixelSize(R.dimen.dashboard_item_size);
        mTickersContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mTickersDashboardAdapter.getNumColumns() == 0) {
                    final int numColumns =
                            (int) Math.floor(mTickersContainer.getWidth() / (dashboardSpacing + dahboardItemSize));
                    if (numColumns > 0) {
                        mTickersDashboardAdapter.setNumColumns(numColumns);
                        mTickersContainer.setNumColumns(numColumns);
                    }
                }
            }
        });
        mTickersDashboardAdapter = new TickersDashboardAdapter(getActivity(), this);
        updateStorageWithTickers();
        mTickersDashboardAdapter.update();
        mTickersContainer.setAdapter(mTickersDashboardAdapter);
        mTickersContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });

        //Broadcast receiver initialization
        mGetStatsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isVisible()) {
                    mTickersDashboardAdapter.update();
                }
            }
        };

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(mGetStatsReceiver, new IntentFilter("UpdateTickers"));

        //Trade listener, once "Buy" or "Sell" clicked, send the order to server
        View.OnClickListener tradeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RegisterTradeRequestTask().execute((v.getId() == R.id.BuyButton) ? "buy" : "sell");
            }
        };

        Button SellButton = (Button) getView().findViewById(R.id.SellButton);
        Button BuyButton = (Button) getView().findViewById(R.id.BuyButton);
        SellButton.setOnClickListener(tradeListener);
        BuyButton.setOnClickListener(tradeListener);

        Button UpdateAccountInfoButton = (Button) getView().findViewById(R.id.UpdateAccountInfoButton);

        UpdateAccountInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateFundsTask().execute();
            }
        });

        //start service to get new data once Dashboard is opened
        getActivity().sendBroadcast(new Intent(getActivity(), StartServiceReceiver.class));
    }

    /**
     * Updates TickerStorage with new tickers
     */
    private void updateStorageWithTickers() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> pairs = preferences.getStringSet("PairsToDisplay", new HashSet<String>());
        for (String pair : pairs) {
            if (!TickersStorage.loadLatestData().containsKey(pair.replace("/", "_").toLowerCase(Locale.US))) {
                Ticker ticker = new Ticker(pair);
                TickersStorage.addNewTicker(ticker);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_menu_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //add pair to dashboard action
            case R.id.action_add:
                final CheckBoxListAdapter checkBoxListAdapter = new CheckBoxListAdapter(getActivity(),
                        getResources().getStringArray(R.array.ExchangePairs),
                        CheckBoxListAdapter.SettingsScope.PAIRS);
                ListView listView = new ListView(getActivity());
                listView.setAdapter(checkBoxListAdapter);
                new AlertDialog.Builder(getActivity())
                        .setTitle(this.getString(R.string.SelectPairsPromptTitle))
                        .setView(listView)
                        .setNeutralButton(getResources().getString(R.string.DialogSaveButton),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        checkBoxListAdapter.saveValuesToPreferences();
                                        updateStorageWithTickers();
                                        mTickersDashboardAdapter.update();
                                        getActivity().sendBroadcast(new Intent(getActivity(),
                                                StartServiceReceiver.class));
                                    }
                                }
                        )
                        .show();
                break;
            //refresh dashboard action
            case R.id.action_refresh:
                getActivity().sendBroadcast(new Intent(getActivity(), StartServiceReceiver.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mGetStatsReceiver);
        super.onDestroyView();
    }

    /**
     * Refreshes funds table with fetched data
     *
     * @param response JSONObject with funds data
     */
    private void refreshFunds(JSONObject response) {
        try {
            if (response == null) {
                Toast.makeText(getActivity(), getResources().getString(R.string.GeneralErrorText), Toast.LENGTH_LONG).show();
                return;
            }
            String notificationText;
            if (response.getInt("success") == 1) {

                View.OnClickListener fillAmount = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ScrollView scrollView = (ScrollView) getView();
                        if (scrollView != null) {
                            EditText tradeAmount = (EditText) scrollView.findViewById(R.id.TradeAmount);
                            tradeAmount.setText(((TextView) v).getText());
                            scrollView.smoothScrollTo(0, scrollView.findViewById(R.id.tradingSection).getBottom());
                        }
                    }
                };

                notificationText = getResources().getString(R.string.FundsInfoUpdatedtext);
                TableLayout fundsContainer = (TableLayout) getView().findViewById(R.id.FundsContainer);
                fundsContainer.removeAllViews();
                JSONObject funds = response.getJSONObject("return").getJSONObject("funds");
                JSONArray fundsNames = response.getJSONObject("return").getJSONObject("funds").names();
                List<String> arrayList = new ArrayList<>();

                for (int i = 0; i < fundsNames.length(); i++) {
                    arrayList.add(fundsNames.getString(i));
                }
                Collections.sort(arrayList);
                TableRow.LayoutParams layoutParams = new TableRow
                        .LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);

                for (String anArrayList : arrayList) {

                    TableRow row = new TableRow(getActivity());
                    TextView currency = new TextView(getActivity());
                    TextView amount = new TextView(getActivity());
                    currency.setText(anArrayList.toUpperCase(Locale.US));
                    amount.setText(funds.getString(anArrayList));
                    currency.setLayoutParams(layoutParams);
                    currency.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    currency.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    currency.setGravity(Gravity.CENTER);
                    amount.setLayoutParams(layoutParams);
                    amount.setGravity(Gravity.CENTER);
                    amount.setOnClickListener(fillAmount);
                    row.addView(currency);
                    row.addView(amount);
                    fundsContainer.addView(row);
                }

            } else {
                notificationText = response.getString("error");
            }

            mCallback.makeNotification(ConstantHolder.ACCOUNT_INFO_NOTIF_ID, notificationText);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPriceClicked(String pair, double price) {
        try {
            ScrollView scrollView = (ScrollView) getView();
            if (scrollView != null) {
                scrollView.smoothScrollTo(0, scrollView.findViewById(R.id.tradingSection).getBottom());
                String[] currencies = pair.split("/");
                EditText tradePrice = (EditText) scrollView.findViewById(R.id.TradePrice);
                tradePrice.setText(String.valueOf(price));
                Spinner tradeCurrency = (Spinner) scrollView.findViewById(R.id.TradeCurrency);
                Spinner tradePriceCurrency = (Spinner) scrollView.findViewById(R.id.TradePriceCurrency);
                tradeCurrency.setSelection(
                        ((ArrayAdapter<String>) tradeCurrency.getAdapter())
                                .getPosition(currencies[0]));
                tradePriceCurrency.setSelection(
                        ((ArrayAdapter<String>) tradePriceCurrency.getAdapter())
                                .getPosition(currencies[1]));
            }
        } catch (ClassCastException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * AsyncTask to register trade request on the exchange
     */
    private class RegisterTradeRequestTask extends AsyncTask<String, Void, JSONObject> {

        private Context mContext;

        @Override
        protected JSONObject doInBackground(String... params) {
            mContext = getActivity().getApplicationContext();
            String tradeAmount = ((EditText) getView().findViewById(R.id.TradeAmount))
                    .getText().toString();
            String tradeCurrency = ((Spinner) getView().findViewById(R.id.TradeCurrency))
                    .getSelectedItem().toString();
            String tradePrice = ((EditText) getView().findViewById(R.id.TradePrice))
                    .getText().toString();
            String tradePriceCurrency = ((Spinner) getView().findViewById(R.id.TradePriceCurrency))
                    .getSelectedItem().toString();
            String tradeAction = params[0];
            String pair = tradeCurrency.toLowerCase(Locale.US) + "_" + tradePriceCurrency.toLowerCase(Locale.US);
            JSONObject response = null;
            try {
                response = MainActivity.app.trade(pair, tradeAction, tradePrice, tradeAmount);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            String message;
            if (jsonObject != null && isVisible()) {
                if (jsonObject.optInt("success") == 1) {
                    message = "Order was successfully added";
                    refreshFunds(jsonObject);
                } else {
                    message = jsonObject.optString("error");
                }
                mCallback.makeNotification(ConstantHolder.TRADE_REGISTERED_NOTIF_ID, message);
            } else {
                Toast.makeText(mContext, getResources()
                        .getString(R.string.GeneralErrorText), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * AsyncTask to update funds
     */
    private class UpdateFundsTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {
            JSONObject response = null;
            try {
                response = MainActivity.app.getAccountInfo();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (isVisible()) {
                refreshFunds(jsonObject);
            }
        }
    }
}

