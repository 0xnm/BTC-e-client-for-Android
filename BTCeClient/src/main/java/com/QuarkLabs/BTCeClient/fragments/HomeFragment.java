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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
import android.widget.AdapterView;
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

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.InMemoryStorage;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter;
import com.QuarkLabs.BTCeClient.adapters.TickersDashboardAdapter;
import com.QuarkLabs.BTCeClient.api.AccountInfo;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.TradeResponse;
import com.QuarkLabs.BTCeClient.interfaces.ActivityCallbacks;
import com.QuarkLabs.BTCeClient.api.Ticker;
import com.QuarkLabs.BTCeClient.services.CheckTickersService;
import com.QuarkLabs.BTCeClient.views.FixedGridView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class HomeFragment extends Fragment implements
        TickersDashboardAdapter.TickersDashboardAdapterCallbackInterface {

    private static final String TRADE_REQUEST_KEY = "TRADE_REQUEST";
    private FixedGridView tickersContainer;
    private TickersDashboardAdapter tickersAdapter;
    private BroadcastReceiver statsReceiver;
    private ActivityCallbacks activityCallback;
    private MenuItem refreshItem;
    private TradeRequest tradeRequest;

    private TextView operationCostView;
    private EditText tradeAmountView;
    private Spinner tradeCurrencyView;
    private EditText tradePriceView;
    private Spinner tradePriceCurrencyView;

    private final TextWatcher tradeAmountWatcher = new TradeConditionWatcher();
    private final TextWatcher tradePriceWatcher = new TradeConditionWatcher();
    private AlertDialog pairsDialog;

    private AppPreferences appPreferences;
    private InMemoryStorage inMemoryStorage;

    @NonNull
    private Queue<Runnable> pendingTasks = new LinkedList<>();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            activityCallback = (ActivityCallbacks) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString() + " must implement "
                    + ActivityCallbacks.class.getSimpleName(), e);
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

        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();
        inMemoryStorage = BtcEApplication.get(getActivity()).getInMemoryStorage();

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
        refreshDashboardAdapter();
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
                    refreshDashboardAdapter();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConstantHolder.UPDATE_TICKERS_ACTION);
        intentFilter.addAction(ConstantHolder.UPDATE_TICKERS_FAILED_ACTION);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext())
                .registerReceiver(statsReceiver, intentFilter);

        tradeAmountView = (EditText) view.findViewById(R.id.TradeAmount);

        List<String> currencies = new ArrayList<>(appPreferences.getExchangeCurrencies());
        ArrayAdapter<String> currenciesAdapter = new ArrayAdapter<>(
                getActivity(), android.R.layout.simple_spinner_item, currencies);
        currenciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tradeCurrencyView = (Spinner) view.findViewById(R.id.TradeCurrency);
        tradeCurrencyView.setAdapter(currenciesAdapter);

        tradePriceView = (EditText) view.findViewById(R.id.TradePrice);

        tradePriceCurrencyView = (Spinner) view.findViewById(R.id.TradePriceCurrency);
        tradePriceCurrencyView.setAdapter(currenciesAdapter);

        operationCostView = (TextView) view.findViewById(R.id.operation_cost);

        //Trade listener, once "Buy" or "Sell" clicked, send the order to server
        View.OnClickListener tradeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tradeAmount = tradeAmountView.getText().toString();
                String tradeCurrency = tradeCurrencyView.getSelectedItem().toString();
                String tradePrice = tradePriceView.getText().toString();
                String tradePriceCurrency = tradePriceCurrencyView.getSelectedItem().toString();

                if (tradeAmount.trim().isEmpty() || tradeCurrency.isEmpty()
                        || tradePrice.trim().isEmpty() || tradePriceCurrency.isEmpty()) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.missing_mandatory_fields_error))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }

                tradeRequest = new TradeRequest(
                        (v.getId() == R.id.BuyButton) ? "buy" : "sell",
                        tradeAmount, tradeCurrency,
                        tradePrice, tradePriceCurrency);
                showTradeRequestDialog(tradeRequest);
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

        if (savedInstanceState != null && savedInstanceState.containsKey(TRADE_REQUEST_KEY)) {
            tradeRequest = savedInstanceState.getParcelable(TRADE_REQUEST_KEY);
            //noinspection ConstantConditions
            showTradeRequestDialog(tradeRequest);
        }

        //start service to get new data once Dashboard is opened
        getActivity().startService(new Intent(getActivity(), CheckTickersService.class));

        if (inMemoryStorage.getFunds() != null) {
            refreshFundsView(inMemoryStorage.getFunds());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        tradeAmountView.addTextChangedListener(tradeAmountWatcher);
        tradePriceView.addTextChangedListener(tradePriceWatcher);
        tradePriceCurrencyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshOperationCostView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // not interested
            }
        });
        while (!pendingTasks.isEmpty()) {
            pendingTasks.poll().run();
        }
    }

    private void refreshOperationCostView() {
        String tradeAmount = tradeAmountView.getText().toString();
        String tradePrice = tradePriceView.getText().toString();
        if (tradeAmount.isEmpty() || tradePrice.isEmpty()) {
            operationCostView.setText("");
        } else {
            try {
                operationCostView.setText(
                        Html.fromHtml(
                                getString(
                                        R.string.trade_operation_cost,
                                        new BigDecimal(tradeAmount)
                                                .multiply(new BigDecimal(tradePrice))
                                                .toPlainString(),
                                        tradePriceCurrencyView.getSelectedItem()
                                )
                        )
                );
            } catch (NumberFormatException nfe) {
                operationCostView.setText("");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        tradeAmountView.removeTextChangedListener(tradeAmountWatcher);
        tradePriceView.removeTextChangedListener(tradePriceWatcher);
        tradePriceCurrencyView.setOnItemSelectedListener(null);
    }

    private void showTradeRequestDialog(@NonNull TradeRequest request) {
        @StringRes int confirmationRes;
        if ("buy".equals(request.type)) {
            confirmationRes = R.string.buy_confirmation;
        } else {
            confirmationRes = R.string.sell_confirmation;
        }
        BigDecimal total = new BigDecimal(request.tradeAmount)
                .multiply(new BigDecimal(request.tradePrice));
        BigDecimal fee = total.multiply(new BigDecimal("0.002"));
        new AlertDialog.Builder(getActivity())
                .setMessage(Html.fromHtml(getString(confirmationRes,
                        request.tradeAmount, request.tradeCurrency,
                        request.tradePrice, request.tradePriceCurrency,
                        request.tradeCurrency,
                        total.toPlainString(),
                        total.subtract(fee).toPlainString(),
                        fee.toPlainString(),
                        request.tradePriceCurrency)))
                .setCancelable(false)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        tradeRequest = null;
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new RegisterTradeRequestTask()
                                .execute(tradeRequest);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Updates TickerStorage with new tickers
     */
    private void updateStorageWithTickers() {
        List<String> pairs = appPreferences.getPairsToDisplay();
        if (pairs.isEmpty()) {
            //cleanup storage
            inMemoryStorage.getLatestData().clear();
            inMemoryStorage.getPreviousData().clear();
            return;
        }
        //checking for added tickers
        for (String pair : pairs) {
            if (!inMemoryStorage.getLatestData()
                    .containsKey(PairUtils.localToServer(pair))) {
                Ticker ticker = new Ticker(pair);
                inMemoryStorage.addNewTicker(ticker);
            }
        }
        //checking for deleted tickers
        for (Iterator<String> iterator = inMemoryStorage.getLatestData().keySet().<String>iterator();
             iterator.hasNext();) {
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
                final PairsCheckboxAdapter pairsCheckboxAdapter = new PairsCheckboxAdapter(
                        getActivity(),
                        appPreferences.getExchangePairs(),
                        PairsCheckboxAdapter.SettingsScope.PAIRS);
                ListView listView = new ListView(getActivity());
                listView.setAdapter(pairsCheckboxAdapter);
                pairsDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(this.getString(R.string.SelectPairsPromptTitle))
                        .setView(listView)
                        .setNeutralButton(R.string.DialogSaveButton,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pairsCheckboxAdapter.saveValuesToPreferences();
                                        updateStorageWithTickers();
                                        refreshDashboardAdapter();
                                        getActivity().startService(new Intent(getActivity(),
                                                CheckTickersService.class));
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
                getActivity().startService(new Intent(getActivity(), CheckTickersService.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshDashboardAdapter() {
        Map<String, Ticker> latestTickers = inMemoryStorage.getLatestData();
        Set<String> dashboardPairs = new HashSet<>(appPreferences.getPairsToDisplay());
        List<Ticker> dashboardTickers = new ArrayList<>();
        for (String pair : latestTickers.keySet()) {
            if (dashboardPairs.contains(pair)) {
                dashboardTickers.add(latestTickers.get(pair));
            }
        }
        tickersAdapter.update(dashboardTickers);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tradeRequest != null) {
            outState.putParcelable(TRADE_REQUEST_KEY, tradeRequest);
        }
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(statsReceiver);
        if (pairsDialog != null) {
            pairsDialog.dismiss();
            pairsDialog = null;
        }
        super.onDestroyView();
    }

    private void refreshFundsView(@NonNull Map<String, BigDecimal> funds) {

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
        Collections.sort(currencies, PairUtils.CURRENCY_COMPARATOR);

        TableRow.LayoutParams layoutParams = new TableRow
                .LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);

        for (String currency : currencies) {

            TableRow row = new TableRow(getActivity());
            TextView currencyView = new TextView(getActivity());
            TextView amountView = new TextView(getActivity());
            currencyView.setText(currency);
            amountView.setText(funds.get(currency).toPlainString());
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
    public void onPriceClicked(@NonNull String pair, @NonNull BigDecimal price) {
        final ScrollView scrollView = (ScrollView) getView();
        if (scrollView != null) {
            String[] currencies = pair.split("/");
            EditText tradePrice = (EditText) scrollView.findViewById(R.id.TradePrice);
            tradePrice.setText(price.toPlainString());
            Spinner tradeCurrency = (Spinner) scrollView.findViewById(R.id.TradeCurrency);
            Spinner tradePriceCurrency
                    = (Spinner) scrollView.findViewById(R.id.TradePriceCurrency);
            tradeCurrency.setSelection(
                    ((ArrayAdapter<String>) tradeCurrency.getAdapter())
                            .getPosition(currencies[0]));
            tradePriceCurrency.setSelection(
                    ((ArrayAdapter<String>) tradePriceCurrency.getAdapter())
                            .getPosition(currencies[1]));

            int y = scrollView.findViewById(R.id.tradingSection).getBottom();
            if (y != 0) {
                scrollView.smoothScrollTo(0, y);
            } else {
                getView().getViewTreeObserver()
                        .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                View view = getView();
                                if (view != null) {
                                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                                }
                                scrollView.smoothScrollTo(0,
                                        scrollView.findViewById(R.id.tradingSection).getBottom());
                                return true;
                            }
                        });
            }
        }
    }

    /**
     * Will be run on the next {@link #onResume()}
     *
     * @param pair  Pair to put in Trading section
     * @param price Price
     */
    public void addShowTradingTask(@NonNull final String pair, @NonNull final BigDecimal price) {
        pendingTasks.add(new Runnable() {
            @Override
            public void run() {
                onPriceClicked(pair, price);
            }
        });
    }

    /**
     * AsyncTask to register trade request on the exchange
     */
    private class RegisterTradeRequestTask extends AsyncTask<TradeRequest, Void,
            CallResult<TradeResponse>> {

        @Override
        protected CallResult<TradeResponse> doInBackground(TradeRequest... params) {
            TradeRequest tradeRequest = params[0];
            String tradeAction = tradeRequest.type;
            String pair = tradeRequest.tradeCurrency.toLowerCase(Locale.US)
                    + "_" + tradeRequest.tradePriceCurrency.toLowerCase(Locale.US);
            return BtcEApplication.get(HomeFragment.this.getActivity()).getApi()
                    .trade(pair, tradeAction, tradeRequest.tradePrice, tradeRequest.tradeAmount);
        }

        @Override
        protected void onPostExecute(@NonNull CallResult<TradeResponse> callResult) {
            String message;
            if (callResult.isSuccess()) {
                message = getString(R.string.order_successfully_added);
                if (isVisible()) {
                    Map<String, BigDecimal> funds = callResult.getPayload().getFunds();
                    inMemoryStorage.setFunds(funds);
                    refreshFundsView(funds);
                }
            } else {
                message = callResult.getError();
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG)
                            .show();
                }
            }
            if (activityCallback != null) {
                activityCallback.makeNotification(ConstantHolder.TRADE_REGISTERED_NOTIF_ID,
                        message);
            }
        }
    }

    /**
     * AsyncTask to update funds
     */
    private class UpdateFundsTask extends AsyncTask<Void, Void, CallResult<AccountInfo>> {

        @Override
        protected CallResult<AccountInfo> doInBackground(Void... params) {
            Context context = getActivity();
            if (context == null) {
                return null;
            }
            return BtcEApplication.get(context).getApi().getAccountInfo();
        }

        @Override
        protected void onPostExecute(CallResult<AccountInfo> result) {
            String notificationText;
            if (result == null || !isVisible()) {
                return;
            }
            if (result.isSuccess()) {
                notificationText = getString(R.string.FundsInfoUpdatedtext);
                Map<String, BigDecimal> funds = result.getPayload().getFunds();
                inMemoryStorage.setFunds(funds);
                refreshFundsView(funds);
            } else {
                notificationText = result.getError();
            }
            if (activityCallback != null) {
                activityCallback.makeNotification(ConstantHolder.ACCOUNT_INFO_NOTIF_ID,
                        notificationText);
            }
        }
    }

    private final class TradeConditionWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // not interested
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // not interested
        }

        @Override
        public void afterTextChanged(Editable s) {
            refreshOperationCostView();
        }
    }

    private static final class TradeRequest implements Parcelable {
        @NonNull
        final String type;
        @NonNull
        final String tradeAmount;
        @NonNull
        final String tradeCurrency;
        @NonNull
        final String tradePrice;
        @NonNull
        final String tradePriceCurrency;

        TradeRequest(@NonNull String type,
                     @NonNull String tradeAmount, @NonNull String tradeCurrency,
                     @NonNull String tradePrice, @NonNull String tradePriceCurrency) {
            this.type = type;
            this.tradeAmount = tradeAmount;
            this.tradeCurrency = tradeCurrency;
            this.tradePrice = tradePrice;
            this.tradePriceCurrency = tradePriceCurrency;
        }

        protected TradeRequest(Parcel in) {
            type = in.readString();
            tradeAmount = in.readString();
            tradeCurrency = in.readString();
            tradePrice = in.readString();
            tradePriceCurrency = in.readString();
        }

        public static final Creator<TradeRequest> CREATOR = new Creator<TradeRequest>() {
            @Override
            public TradeRequest createFromParcel(Parcel in) {
                return new TradeRequest(in);
            }

            @Override
            public TradeRequest[] newArray(int size) {
                return new TradeRequest[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(type);
            dest.writeString(tradeAmount);
            dest.writeString(tradeCurrency);
            dest.writeString(tradePrice);
            dest.writeString(tradePriceCurrency);
        }
    }
}

