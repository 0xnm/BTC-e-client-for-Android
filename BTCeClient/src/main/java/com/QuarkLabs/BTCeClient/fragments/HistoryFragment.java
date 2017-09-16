package com.QuarkLabs.BTCeClient.fragments;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.ListType;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.TradesAdapter;
import com.QuarkLabs.BTCeClient.adapters.TransactionsAdapter;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.TradeHistoryEntry;
import com.QuarkLabs.BTCeClient.api.Transaction;
import com.QuarkLabs.BTCeClient.loaders.TradesLoader;
import com.QuarkLabs.BTCeClient.loaders.TransactionsLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

public class HistoryFragment extends Fragment {

    private static final String ERROR_TAG = HistoryFragment.class.getSimpleName();

    private static final String START_DATE_KEY = "startDate";
    private static final String END_DATE_KEY = "endDate";

    private static final long WEEK_AS_MILLIS = TimeUnit.DAYS.toMillis(7);
    private static final String LIST_TYPE_KEY = "ListType";
    private int mLoaderId;

    private DateFormat dateFormat;

    private TransactionsAdapter transactionsAdapter = new TransactionsAdapter();
    private TradesAdapter tradesAdapter = new TradesAdapter();

    private LoaderManager.LoaderCallbacks loaderCallbacks;

    private ListView historyView;
    private Date startDateValue;
    private Date endDateValue;
    private ProgressBar loadingView;
    private TextView errorView;

    public static HistoryFragment newInstance(@NonNull ListType historyType) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(LIST_TYPE_KEY, historyType);
        HistoryFragment fragment = new HistoryFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
        ListType historyType = (ListType) getArguments().getSerializable(LIST_TYPE_KEY);
        mLoaderId = historyType.ordinal();

        //if we have StartDate and EndDate selected before
        if (savedInstanceState != null) {
            try {
                startDateValue = dateFormat.parse(savedInstanceState.getString(START_DATE_KEY));
                endDateValue = dateFormat.parse(savedInstanceState.getString(END_DATE_KEY));
            } catch (ParseException pe) {
                Log.e(ERROR_TAG, "Failed to parse saved start/end dates", pe);
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            startDateValue = new Date(calendar.getTimeInMillis() - WEEK_AS_MILLIS);
            endDateValue = new Date(calendar.getTimeInMillis());
        }

        if (historyType == ListType.Transactions) {
            loaderCallbacks = new TransactionsLoaderCallbacks();
        } else {
            loaderCallbacks = new TradesLoaderCallbacks();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle bundle = new Bundle();
        bundle.putString(START_DATE_KEY, String.valueOf(startDateValue.getTime() / 1000));
        bundle.putString(END_DATE_KEY, String.valueOf(endDateValue.getTime() / 1000));
        getLoaderManager().initLoader(mLoaderId, bundle, loaderCallbacks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trade_trans_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        historyView = (ListView) view.findViewById(R.id.HistoryContainer);
        final EditText startDate = (EditText) view.findViewById(R.id.StartDateValue);
        final EditText endDate = (EditText) view.findViewById(R.id.EndDateValue);

        startDate.setText(dateFormat.format(startDateValue));
        endDate.setText(dateFormat.format(endDateValue));
        Button queryButton = (Button) view.findViewById(R.id.MakeQueryButton);

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(startDate);
            }
        });
        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(endDate);
            }
        });

        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(START_DATE_KEY, String.valueOf(startDateValue.getTime() / 1000));
                bundle.putString(END_DATE_KEY, String.valueOf(endDateValue.getTime() / 1000));
                getLoaderManager().restartLoader(mLoaderId, bundle, loaderCallbacks);
                historyView.setAdapter(null);
                errorView.setVisibility(View.GONE);
                historyView.setEmptyView(loadingView);

            }
        });

        loadingView = (ProgressBar) view.findViewById(R.id.Loading);
        errorView = (TextView) view.findViewById(R.id.NoItems);
        historyView.setEmptyView(loadingView);
    }

    private void showDatePicker(final TextView origin) {
        Date date = null;
        try {
            date = dateFormat.parse(origin.getText().toString());
        } catch (ParseException e) {
            Log.e(HistoryFragment.class.getSimpleName(), "Failed to parse datetime", e);
        }
        int year = 1999;
        int month = 0;
        int day = 1;
        if (date != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        Calendar calendar = Calendar.getInstance();
                        if (origin.getId() == R.id.StartDateValue) {
                            calendar.setTime(startDateValue);
                            calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                            startDateValue = calendar.getTime();
                        } else {
                            calendar.setTime(endDateValue);
                            calendar.set(year, monthOfYear, dayOfMonth, 23, 59, 59);
                            endDateValue = calendar.getTime();
                        }
                        origin.setText(dateFormat.format(calendar.getTime()));
                    }
                }, year, month, day
        );

        datePickerDialog.getDatePicker().setCalendarViewShown(false);
        datePickerDialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_refresh_filter, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(START_DATE_KEY, dateFormat.format(startDateValue));
        outState.putString(END_DATE_KEY, dateFormat.format(endDateValue));
    }

    private void showServerError(@Nullable String error) {
        Toast.makeText(getActivity(), R.string.general_error_text, Toast.LENGTH_LONG)
                .show();
        if (error == null || error.isEmpty()) {
            error = getString(R.string.OoopsError);
        }
        errorView.setText(error.toUpperCase(Locale.US));
        historyView.setAdapter(null);
        historyView.setEmptyView(errorView);
        loadingView.setVisibility(View.GONE);
    }

    private final class TransactionsLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<CallResult<List<Transaction>>> {

        @Override
        public Loader<CallResult<List<Transaction>>> onCreateLoader(int id, Bundle args) {
            //noinspection ConstantConditions
            return new TransactionsLoader(getActivity(),
                    args.getString(START_DATE_KEY), args.getString(END_DATE_KEY));
        }

        @Override
        public void onLoadFinished(Loader<CallResult<List<Transaction>>> loader,
                                   CallResult<List<Transaction>> data) {
            if (data.isSuccess()) {
                if (!data.getPayload().isEmpty()) {
                    //noinspection ConstantConditions
                    transactionsAdapter.setTransactions(data.getPayload());
                    historyView.setAdapter(transactionsAdapter);
                } else {
                    historyView.setAdapter(null);
                    errorView.setText(getString(R.string.no_items_text).toUpperCase(Locale.US));
                    historyView.setEmptyView(errorView);
                }
            } else {
                showServerError(data.getError());
            }
            loadingView.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<CallResult<List<Transaction>>> loader) {
            historyView.setAdapter(null);
            errorView.setVisibility(View.GONE);
            historyView.setEmptyView(loadingView);
        }
    }

    private final class TradesLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<CallResult<List<TradeHistoryEntry>>> {

        @Override
        public Loader<CallResult<List<TradeHistoryEntry>>> onCreateLoader(int id, Bundle args) {
            //noinspection ConstantConditions
            return new TradesLoader(getActivity(),
                    args.getString(START_DATE_KEY), args.getString(END_DATE_KEY));
        }

        @Override
        public void onLoadFinished(Loader<CallResult<List<TradeHistoryEntry>>> loader,
                                   CallResult<List<TradeHistoryEntry>> data) {
            if (data.isSuccess()) {
                // workaround for trade history bug
                long start = startDateValue.getTime() / 1000L;
                List<TradeHistoryEntry> filteredTrades = new ArrayList<>();
                for (TradeHistoryEntry trade : data.getPayload()) {
                    if (trade.getTimestamp() >= start) {
                        filteredTrades.add(trade);
                    }
                }
                if (!filteredTrades.isEmpty()) {
                    //noinspection ConstantConditions
                    tradesAdapter.setTrades(data.getPayload());
                    historyView.setAdapter(tradesAdapter);
                } else {
                    historyView.setAdapter(null);
                    errorView.setText(getString(R.string.no_items_text).toUpperCase(Locale.US));
                    historyView.setEmptyView(errorView);
                }
            } else {
                showServerError(data.getError());
            }
            loadingView.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<CallResult<List<TradeHistoryEntry>>> loader) {
            historyView.setAdapter(null);
            errorView.setVisibility(View.GONE);
            historyView.setEmptyView(loadingView);
        }
    }
}
