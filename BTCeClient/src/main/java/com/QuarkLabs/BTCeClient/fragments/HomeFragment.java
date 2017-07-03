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
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.StartServiceReceiver;
import com.QuarkLabs.BTCeClient.TickersStorage;
import com.QuarkLabs.BTCeClient.adapters.CheckBoxListAdapter;
import com.QuarkLabs.BTCeClient.adapters.TickersDashboardAdapter;
import com.QuarkLabs.BTCeClient.api.AccountInfo;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.TradeResponse;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;
import com.QuarkLabs.BTCeClient.api.Ticker;
import com.QuarkLabs.BTCeClient.views.FixedGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements
        TickersDashboardAdapter.TickersDashboardAdapterCallbackInterface {

    private FixedGridView tickersContainer;
    private TickersDashboardAdapter tickersAdapter;
    private BroadcastReceiver statsReceiver;
    private ActivityCallbacks activityCallback;
    private MenuItem refreshItem;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            activityCallback = (ActivityCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                    + ActivityCallbacks.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        activityCallback = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        tickersContainer = (FixedGridView) view.findViewById(R.id.tickersContainer);
        tickersContainer.setExpanded(true);
        final int dashboardSpacing = getResources()
                .getDimensionPixelSize(R.dimen.dashboard_spacing);
        final int dashboardItemSize = getResources()
                .getDimensionPixelSize(R.dimen.dashboard_item_size);
        tickersContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (tickersAdapter.getNumColumns() == 0) {
                    final int numColumns =
                            (int) Math.floor(tickersContainer.getWidth() /
                                    (dashboardSpacing + dashboardItemSize));
                    if (numColumns > 0) {
                        tickersAdapter.setNumColumns(numColumns);
                        tickersContainer.setNumColumns(numColumns);
                    }
                }
            }
        });
        tickersAdapter = new TickersDashboardAdapter(getActivity(), this);
        updateStorageWithTickers();
        tickersAdapter.update();
        tickersContainer.setAdapter(tickersAdapter);
        TextView emptyView = (TextView) view.findViewById(R.id.emptyView);
        tickersContainer.setEmptyView(emptyView);
        tickersContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });

        //Broadcast receiver initialization
        statsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isVisible()) {
                    if (refreshItem != null) {
                        refreshItem.collapseActionView();
                        refreshItem.setActionView(null);
                    }
                    tickersAdapter.update();
                }
            }
        };

        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(statsReceiver,
                        new IntentFilter(ConstantHolder.UPDATE_TICKERS_ACTION));

        //Trade listener, once "Buy" or "Sell" clicked, send the order to server
        View.OnClickListener tradeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RegisterTradeRequestTask()
                        .execute((v.getId() == R.id.BuyButton) ? "buy" : "sell");
            }
        };

        view.findViewById(R.id.SellButton).setOnClickListener(tradeListener);
        view.findViewById(R.id.BuyButton).setOnClickListener(tradeListener);

        Button updateAccountInfoButton = (Button) view.findViewById(R.id.UpdateAccountInfoButton);

        updateAccountInfoButton.setOnClickListener(new View.OnClickListener() {
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
        List<String> pairs = PairUtils.getTickersToDisplayThatSupported(getActivity());
        if (pairs.isEmpty()) {
            //cleanup storage
            TickersStorage.loadLatestData().clear();
            TickersStorage.loadPreviousData().clear();
            return;
        }
        //checking for added tickers
        for (String pair : pairs) {
            if (!TickersStorage.loadLatestData()
                    .containsKey(pair.replace("/", "_").toLowerCase(Locale.US))) {
                Ticker ticker = new Ticker(pair);
                TickersStorage.addNewTicker(ticker);
            }
        }
        //checking for deleted tickers
        for (Iterator<String> iterator = TickersStorage
                .loadLatestData().keySet().<String>iterator();
             iterator.hasNext(); ) {
            String key = iterator.next();
            if (!pairs.contains(key)) {
                iterator.remove();
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
                final CheckBoxListAdapter checkBoxListAdapter = new CheckBoxListAdapter(
                        getActivity(),
                        getResources().getStringArray(R.array.ExchangePairs),
                        CheckBoxListAdapter.SettingsScope.PAIRS);
                ListView listView = new ListView(getActivity());
                listView.setAdapter(checkBoxListAdapter);
                new AlertDialog.Builder(getActivity())
                        .setTitle(this.getString(R.string.SelectPairsPromptTitle))
                        .setView(listView)
                        .setNeutralButton(R.string.DialogSaveButton,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        checkBoxListAdapter.saveValuesToPreferences();
                                        updateStorageWithTickers();
                                        tickersAdapter.update();
                                        getActivity().sendBroadcast(new Intent(getActivity(),
                                                StartServiceReceiver.class));
                                    }
                                }
                        )
                        .show();
                break;
            //refresh dashboard action
            case R.id.action_refresh:
                refreshItem = item;
                refreshItem.setActionView(R.layout.progress_bar_action_view);
                refreshItem.expandActionView();
                getActivity().sendBroadcast(new Intent(getActivity(), StartServiceReceiver.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(statsReceiver);
        super.onDestroyView();
    }

    private void refreshFundsView(@NonNull Map<String, Double> funds) {

        View.OnClickListener fillAmount = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScrollView scrollView = (ScrollView) getView();
                if (scrollView != null) {
                    EditText tradeAmount
                            = (EditText) scrollView.findViewById(R.id.TradeAmount);
                    tradeAmount.setText(((TextView) v).getText());
                    scrollView.smoothScrollTo(
                            0, scrollView.findViewById(R.id.tradingSection).getBottom());
                }
            }
        };

        TableLayout fundsContainer
                = (TableLayout) getView().findViewById(R.id.FundsContainer);
        fundsContainer.removeAllViews();

        List<String> currencies = new ArrayList<>(funds.keySet());
        Collections.sort(currencies);

        TableRow.LayoutParams layoutParams = new TableRow
                .LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);

        for (String currency : currencies) {

            TableRow row = new TableRow(getActivity());
            TextView currencyView = new TextView(getActivity());
            TextView amountView = new TextView(getActivity());
            currencyView.setText(currency.toUpperCase(Locale.US));
            amountView.setText(String.valueOf(funds.get(currency)));
            currencyView.setLayoutParams(layoutParams);
            currencyView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            currencyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            currencyView.setGravity(Gravity.CENTER);
            amountView.setLayoutParams(layoutParams);
            amountView.setGravity(Gravity.CENTER);
            amountView.setOnClickListener(fillAmount);
            row.addView(currencyView);
            row.addView(amountView);
            fundsContainer.addView(row);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPriceClicked(String pair, double price) {
        try {
            ScrollView scrollView = (ScrollView) getView();
            if (scrollView != null) {
                scrollView.smoothScrollTo(
                        0, scrollView.findViewById(R.id.tradingSection).getBottom());
                String[] currencies = pair.split("/");
                EditText tradePrice = (EditText) scrollView.findViewById(R.id.TradePrice);
                tradePrice.setText(String.valueOf(price));
                Spinner tradeCurrency = (Spinner) scrollView.findViewById(R.id.TradeCurrency);
                Spinner tradePriceCurrency
                        = (Spinner) scrollView.findViewById(R.id.TradePriceCurrency);
                tradeCurrency.setSelection(
                        ((ArrayAdapter<String>) tradeCurrency.getAdapter())
                                .getPosition(currencies[0]));
                tradePriceCurrency.setSelection(
                        ((ArrayAdapter<String>) tradePriceCurrency.getAdapter())
                                .getPosition(currencies[1]));
            }
        } catch (ClassCastException | NullPointerException e) {
            Log.e(HomeFragment.class.getSimpleName(), "Failure on setting trading price", e);
        }
    }

    /**
     * AsyncTask to register trade request on the exchange
     */
    private class RegisterTradeRequestTask extends AsyncTask<String, Void,
            CallResult<TradeResponse>> {

        private volatile String tradeAmount;
        private volatile String tradeCurrency;
        private volatile String tradePrice;
        private volatile String tradePriceCurrency;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            tradeAmount = ((EditText) getView().findViewById(R.id.TradeAmount))
                    .getText().toString();
            tradeCurrency = ((Spinner) getView().findViewById(R.id.TradeCurrency))
                    .getSelectedItem().toString();
            tradePrice = ((EditText) getView().findViewById(R.id.TradePrice))
                    .getText().toString();
            tradePriceCurrency = ((Spinner) getView().findViewById(R.id.TradePriceCurrency))
                    .getSelectedItem().toString();
        }

        @Override
        protected CallResult<TradeResponse> doInBackground(String... params) {
            String tradeAction = params[0];
            String pair = tradeCurrency.toLowerCase(Locale.US)
                    + "_" + tradePriceCurrency.toLowerCase(Locale.US);
            return BtcEApplication.get(HomeFragment.this.getActivity()).getApi()
                    .trade(pair, tradeAction, tradePrice, tradeAmount);
        }

        @Override
        protected void onPostExecute(@NonNull CallResult<TradeResponse> callResult) {
            String message;
            if (callResult.isSuccess()) {
                message = "Order was successfully added";
                if (isVisible()) {
                    refreshFundsView(callResult.getPayload().getFunds());
                }
            } else {
                message = callResult.getError();
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG)
                            .show();
                }
            }
            activityCallback.makeNotification(ConstantHolder.TRADE_REGISTERED_NOTIF_ID,
                    message);
        }
    }

    /**
     * AsyncTask to update funds
     */
    private class UpdateFundsTask extends AsyncTask<Void, Void, CallResult<AccountInfo>> {

        @Override
        protected CallResult<AccountInfo> doInBackground(Void... params) {
            return BtcEApplication.get(getActivity()).getApi().getAccountInfo();
        }

        @Override
        protected void onPostExecute(CallResult<AccountInfo> result) {
            String notificationText;
            if (result.isSuccess()) {
                notificationText = getString(R.string.FundsInfoUpdatedtext);
                if (isVisible()) {
                    refreshFundsView(result.getPayload().getFunds());
                }
            } else {
                notificationText = result.getError();
            }
            activityCallback.makeNotification(ConstantHolder.ACCOUNT_INFO_NOTIF_ID,
                    notificationText);
        }
    }
}

