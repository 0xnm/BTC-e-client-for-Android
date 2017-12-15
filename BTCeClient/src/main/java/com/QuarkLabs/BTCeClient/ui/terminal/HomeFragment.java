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

package com.QuarkLabs.BTCeClient.ui.terminal;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
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

import com.QuarkLabs.BTCeClient.api.AccountInfo;
import com.QuarkLabs.BTCeClient.api.TradeResponse;
import com.QuarkLabs.BTCeClient.data.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.ConstantHolder;
import com.QuarkLabs.BTCeClient.data.InMemoryStorage;
import com.QuarkLabs.BTCeClient.tasks.ApiResultListener;
import com.QuarkLabs.BTCeClient.tasks.RegisterTradeRequestTask;
import com.QuarkLabs.BTCeClient.tasks.TradeRequest;
import com.QuarkLabs.BTCeClient.tasks.UnregistrableTask;
import com.QuarkLabs.BTCeClient.tasks.UpdateFundsTask;
import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter;
import com.QuarkLabs.BTCeClient.adapters.TickersDashboardAdapter;
import com.QuarkLabs.BTCeClient.api.TradeType;
import com.QuarkLabs.BTCeClient.api.Ticker;
import com.QuarkLabs.BTCeClient.services.CheckTickersService;
import com.QuarkLabs.BTCeClient.views.FixedGridView;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class HomeFragment extends Fragment implements
        TickersDashboardAdapter.TickersDashboardAdapterCallbackInterface,
        TradeDialogFragment.TradeRequestListener {

    private static final String TRADE_REQUEST_DIALOG_TAG = "TRADE_REQUEST_DIALOG";

    private static final NumberFormat VALUE_FORMAT;

    static {
        VALUE_FORMAT = DecimalFormat.getNumberInstance(Locale.US);
        VALUE_FORMAT.setGroupingUsed(false);
        VALUE_FORMAT.setMaximumFractionDigits(6);
    }

    private FixedGridView tickersContainer;
    private TickersDashboardAdapter tickersAdapter;
    private BroadcastReceiver statsReceiver;
    private MenuItem refreshItem;

    private EditText tradeAmountView;
    private Spinner tradeCurrencyView;
    private EditText tradePriceView;
    private Spinner tradePriceCurrencyView;
    private EditText totalCostView;
    private TextView totalCostCurrencyView;

    private final TextWatcher amountWatcher = new TradeAmountWatcher();
    private final TextWatcher totalWatcher = new TradeTotalWatcher();
    private final TextWatcher priceWatcher = new PriceWatcher();
    private AlertDialog pairsDialog;

    private AppPreferences appPreferences;
    private InMemoryStorage inMemoryStorage;

    private ViewGroup fundsContainer;
    private ViewGroup tradingContainer;

    @NonNull
    private List<UnregistrableTask> ongoingTasks = new ArrayList<>();
    @NonNull
    private final Queue<Runnable> pendingTasks = new LinkedList<>();


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

        fundsContainer = (ViewGroup) view.findViewById(R.id.FundsContainer);
        tradingContainer = (ViewGroup) view.findViewById(R.id.tradingSection);

        tickersContainer = (FixedGridView) view.findViewById(R.id.tickersContainer);
        tickersContainer.setExpanded(true);
        final int dashboardSpacing = getResources()
                .getDimensionPixelSize(R.dimen.dashboard_spacing);
        final int dashboardItemSize = getResources()
                .getDimensionPixelSize(R.dimen.dashboard_item_size);
        tickersContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> {
                    if (tickersAdapter.getNumColumns() == 0) {
                        final int numColumns =
                                (int) Math.floor(tickersContainer.getWidth() /
                                        (dashboardSpacing + dashboardItemSize));
                        if (numColumns > 0) {
                            tickersAdapter.setNumColumns(numColumns);
                            tickersContainer.setNumColumns(numColumns);
                        }
                    }
                });
        tickersAdapter = new TickersDashboardAdapter(getActivity(), this);
        updateStorageWithTickers();
        refreshDashboardAdapter();
        tickersContainer.setAdapter(tickersAdapter);
        TextView emptyView = (TextView) view.findViewById(R.id.emptyView);
        tickersContainer.setEmptyView(emptyView);
        tickersContainer.setOnTouchListener((v, event) ->
                event.getAction() == MotionEvent.ACTION_MOVE);

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

        totalCostView = (EditText) view.findViewById(R.id.total);
        totalCostCurrencyView = (TextView) view.findViewById(R.id.total_currency);

        view.findViewById(R.id.SellButton).setOnClickListener(this::onTradeRequested);
        view.findViewById(R.id.BuyButton).setOnClickListener(this::onTradeRequested);

        Button updateAccountInfoButton = (Button) view.findViewById(R.id.UpdateAccountInfoButton);

        updateAccountInfoButton.setOnClickListener(v -> sendUpdateFundsTask());

        //start service to get new data once Dashboard is opened
        getActivity().startService(new Intent(getActivity(), CheckTickersService.class));

        if (inMemoryStorage.getFunds() != null) {
            refreshFundsView(inMemoryStorage.getFunds());
        }

        TradeDialogFragment tradeDialog = (TradeDialogFragment) getFragmentManager()
                .findFragmentByTag(TRADE_REQUEST_DIALOG_TAG);
        if (tradeDialog != null) {
            tradeDialog.setListener(this);
        }
    }

    private void onTradeRequested(View v) {
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

        TradeRequest tradeRequest = new TradeRequest(
                (v.getId() == R.id.BuyButton) ? TradeType.BUY : TradeType.SELL,
                tradeAmount, tradeCurrency,
                tradePrice, tradePriceCurrency);
        showTradeRequestDialog(tradeRequest);
    }

    private void sendUpdateFundsTask() {
        UpdateFundsTask task = new UpdateFundsTask(getActivity(),
                BtcEApplication.get(getActivity()).getApi(),
                new ApiResultListener<AccountInfo>() {
                    @Override
                    public void onSuccess(@NonNull AccountInfo result) {
                        Map<String, BigDecimal> funds = inMemoryStorage.getFunds();
                        if (funds != null) {
                            refreshFundsView(funds);
                        }
                    }

                    @Override
                    public void onError(@NonNull String error) {
                        Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
                    }
                });
        task.execute();
        ongoingTasks.add(task);
    }

    private void showTradeRequestDialog(@NonNull TradeRequest tradeRequest) {
        TradeDialogFragment tradeDialog = TradeDialogFragment.create(tradeRequest);
        tradeDialog.setListener(this);
        tradeDialog.show(getFragmentManager(), TRADE_REQUEST_DIALOG_TAG);
    }

    @Override
    public void onResume() {
        super.onResume();
        tradeAmountView.addTextChangedListener(amountWatcher);
        totalCostView.addTextChangedListener(totalWatcher);
        tradePriceView.addTextChangedListener(priceWatcher);
        tradePriceCurrencyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                totalCostCurrencyView.setText((String) parent.getItemAtPosition(position));
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

    @Override
    public void onPause() {
        super.onPause();
        tradeAmountView.removeTextChangedListener(amountWatcher);
        totalCostView.removeTextChangedListener(totalWatcher);
        tradePriceView.removeTextChangedListener(priceWatcher);
        tradePriceCurrencyView.setOnItemSelectedListener(null);
    }

    /**
     * Updates TickerStorage with new tickers
     */
    private void updateStorageWithTickers() {
        List<String> pairs = appPreferences.getPairsToDisplay();
        if (pairs.isEmpty()) {
            //cleanup storage
            inMemoryStorage.clearTickers();
            return;
        }
        //checking for added tickers
        for (String pair : pairs) {
            if (!inMemoryStorage.getLatestData().containsKey(pair)) {
                Ticker ticker = new Ticker(pair);
                inMemoryStorage.addNewTicker(ticker);
            }
        }
        //checking for deleted tickers
        for (Map.Entry<String, Ticker> entry : inMemoryStorage.getLatestData().entrySet()) {
            String pair = entry.getKey();
            if (!pairs.contains(pair)) {
                inMemoryStorage.removeTicker(entry.getValue());
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
                                (dialog, which) -> {
                                    pairsCheckboxAdapter.saveValuesToPreferences();
                                    updateStorageWithTickers();
                                    refreshDashboardAdapter();
                                    getActivity().startService(new Intent(getActivity(),
                                            CheckTickersService.class));
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
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(statsReceiver);
        if (pairsDialog != null) {
            pairsDialog.dismiss();
            pairsDialog = null;
        }
        for (UnregistrableTask task : ongoingTasks) {
            task.unregisterListener();
        }
        super.onDestroyView();
    }

    private void refreshFundsView(@NonNull Map<String, BigDecimal> funds) {

        fundsContainer.removeAllViews();

        if (appPreferences.isDontShowZeroFunds()) {
            funds = PairUtils.filterForNonZero(funds);
        }

        if (funds.isEmpty()) {
            TextView zeroBalanceView = new TextView(getActivity());
            TableLayout.LayoutParams lps = new TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            zeroBalanceView.setGravity(Gravity.CENTER);
            int fourDp = getResources().getDimensionPixelSize(R.dimen.four_dp);
            zeroBalanceView.setLayoutParams(lps);
            zeroBalanceView.setPadding(fourDp, fourDp, fourDp, fourDp);
            zeroBalanceView.setText(R.string.zero_balance);
            zeroBalanceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            fundsContainer.addView(zeroBalanceView);

        } else {
            List<String> currencies = new ArrayList<>(funds.keySet());
            Collections.sort(currencies, PairUtils.CURRENCY_COMPARATOR);

            TableRow.LayoutParams layoutParams = new TableRow
                    .LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);

            for (final String currency : currencies) {

                TableRow row = new TableRow(getActivity());

                TextView currencyView = new TextView(getActivity());
                currencyView.setText(currency);
                currencyView.setLayoutParams(layoutParams);
                currencyView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                currencyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                currencyView.setGravity(Gravity.CENTER);

                TextView amountView = new TextView(getActivity());
                final String amount = funds.get(currency).toPlainString();
                amountView.setText(amount);
                amountView.setLayoutParams(layoutParams);
                amountView.setGravity(Gravity.CENTER);
                amountView.setOnClickListener(v -> goToTrading(amount, currency, null, null));

                row.addView(currencyView);
                row.addView(amountView);
                fundsContainer.addView(row);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPriceClicked(@NonNull String pair, @NonNull BigDecimal price) {
        String[] currencies = pair.split("/");
        goToTrading(null, currencies[0], price.toPlainString(), currencies[1]);
    }

    /**
     * Shows Trading section with given parameters
     *
     * @param tradeAmount        Amount to sell/buy
     * @param tradeCurrency      Currency to sell/buy
     * @param tradePrice         Buying/Selling price
     * @param tradePriceCurrency Currency to buy/sell in
     */
    private void goToTrading(@Nullable String tradeAmount, @Nullable String tradeCurrency,
                             @Nullable String tradePrice, @Nullable String tradePriceCurrency) {
        final ScrollView scrollView = (ScrollView) getView();
        if (scrollView == null) {
            return;
        }

        if (!TextUtils.isEmpty(tradeAmount)) {
            tradeAmountView.setText(tradeAmount);
        }

        if (!TextUtils.isEmpty(tradePrice)) {
            tradePriceView.setText(tradePrice);
        }
        if (!TextUtils.isEmpty(tradeCurrency)) {
            tradeCurrencyView.setSelection(
                    ((ArrayAdapter<String>) tradeCurrencyView.getAdapter())
                            .getPosition(tradeCurrency));
        }
        if (!TextUtils.isEmpty(tradePriceCurrency)) {
            tradePriceCurrencyView.setSelection(
                    ((ArrayAdapter<String>) tradePriceCurrencyView.getAdapter())
                            .getPosition(tradePriceCurrency));
        }

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
                            scrollView.smoothScrollTo(0, tradingContainer.getBottom());
                            return true;
                        }
                    });
        }
    }

    /**
     * Will be run on the next {@link #onResume()}
     *
     * @param pair  Pair to put in Trading section
     * @param price Price
     */
    public void addShowTradingTask(@NonNull final String pair, @NonNull final BigDecimal price) {
        pendingTasks.add(() -> onPriceClicked(pair, price));
    }

    @Override
    public void onSendTradeRequest(@NonNull TradeRequest tradeRequest) {
        BtcEApplication app = BtcEApplication.get(getActivity());
        RegisterTradeRequestTask task = new RegisterTradeRequestTask(app,
                app.getApi(),
                new ApiResultListener<TradeResponse>() {
                    @Override
                    public void onSuccess(@NonNull TradeResponse result) {
                        Map<String, BigDecimal> funds = inMemoryStorage.getFunds();
                        if (funds != null) {
                            refreshFundsView(funds);
                        }
                    }

                    @Override
                    public void onError(@NonNull String error) {
                        Activity activity = getActivity();
                        if (getActivity() != null) {
                            Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
        task.execute(tradeRequest);
        ongoingTasks.add(task);
    }

    private double tryGetDouble(@NonNull String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    private final class TradeAmountWatcher implements TextWatcher {

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
            double price = tryGetDouble(tradePriceView.getText().toString());
            double amount = tryGetDouble(s.toString());
            String total;
            if (price != -1 && amount != -1) {
                total = VALUE_FORMAT.format(price * amount);
            } else {
                total = "";
            }

            totalCostView.removeTextChangedListener(totalWatcher);
            totalCostView.setText(total);
            totalCostView.addTextChangedListener(totalWatcher);
        }
    }

    private final class TradeTotalWatcher implements TextWatcher {

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
            double price = tryGetDouble(tradePriceView.getText().toString());
            double total = tryGetDouble(s.toString());

            if (price != -1 && total != -1) {
                tradeAmountView.removeTextChangedListener(amountWatcher);
                tradeAmountView.setText(VALUE_FORMAT.format(total / price));
                tradeAmountView.addTextChangedListener(amountWatcher);
            }
        }
    }

    private final class PriceWatcher implements TextWatcher {

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
            double amount = tryGetDouble(tradeAmountView.getText().toString());
            double price = tryGetDouble(s.toString());
            String total;
            if (price != -1 && amount != -1) {
                total = VALUE_FORMAT.format(price * amount);
            } else {
                total = "";
            }
            totalCostView.removeTextChangedListener(totalWatcher);
            totalCostView.setText(total);
            totalCostView.addTextChangedListener(totalWatcher);
        }
    }

}

