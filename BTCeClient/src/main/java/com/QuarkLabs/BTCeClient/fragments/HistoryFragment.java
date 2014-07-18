package com.QuarkLabs.BTCeClient.fragments;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.QuarkLabs.BTCeClient.ListType;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.OrdersAdapter;
import com.QuarkLabs.BTCeClient.loaders.OrdersLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

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

public class HistoryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<JSONObject> {

    private static final int WEEK = 7 * 24 * 60 * 60 * 1000;
    private static String mListTypeTag = "ListType";
    private int mLoaderId;
    private java.text.DateFormat mDateFormat;
    private OrdersAdapter mAdapter;
    private ListView mListView;
    private Date mStartDateValue;
    private Date mEndDateValue;
    private ProgressBar mLoadingView;
    private TextView mNoItems;
    private ListType mHistoryType;

    public static HistoryFragment newInstance(ListType historyType) {
        if (historyType == ListType.ActiveOrders) {
            throw new IllegalArgumentException("ActiveOrders type is not supported by this Fragment");
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(mListTypeTag, historyType);
        HistoryFragment myFragment = new HistoryFragment();
        myFragment.setArguments(bundle);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
        mHistoryType = (ListType) getArguments().getSerializable(mListTypeTag);
        mLoaderId = mHistoryType.ordinal();
        //if we have StartDate and EndDate selected before
        if (savedInstanceState != null) {
            try {
                mStartDateValue = mDateFormat.parse(savedInstanceState.getString("startDate"));
                mEndDateValue = mDateFormat.parse(savedInstanceState.getString("endDate"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            mStartDateValue = new Date(calendar.getTimeInMillis() - WEEK);
            mEndDateValue = new Date(calendar.getTimeInMillis());
        }

        //creating bundle for loader
        Bundle bundle = new Bundle();
        bundle.putString("startDate", String.valueOf(mStartDateValue.getTime() / 1000));
        bundle.putString("endDate", String.valueOf(mEndDateValue.getTime() / 1000));
        getLoaderManager().initLoader(mLoaderId, bundle, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trade_trans_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new OrdersAdapter(getActivity(), mHistoryType);
        mListView = (ListView) getView().findViewById(R.id.HistoryContainer);
        final EditText startDate = (EditText) getView().findViewById(R.id.StartDateValue);
        final EditText endDate = (EditText) getView().findViewById(R.id.EndDateValue);
        startDate.setText(mDateFormat.format(mStartDateValue));
        endDate.setText(mDateFormat.format(mEndDateValue));
        Button makeQuery = (Button) getView().findViewById(R.id.MakeQueryButton);
        View.OnClickListener showDatePicker = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView dateValue = (TextView) v;
                Date date = null;
                try {
                    date = mDateFormat.parse(dateValue.getText().toString());
                } catch (ParseException e) {
                    e.printStackTrace();
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

                DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar calendar = Calendar.getInstance();
                                if (dateValue.getId() == R.id.StartDateValue) {
                                    calendar.setTime(mStartDateValue);
                                    calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                                    mStartDateValue = calendar.getTime();
                                } else {
                                    calendar.setTime(mEndDateValue);
                                    calendar.set(year, monthOfYear, dayOfMonth, 23, 59, 59);
                                    mEndDateValue = calendar.getTime();
                                }
                                dateValue.setText(mDateFormat.format(calendar.getTime()));
                            }
                        }, year, month, day
                );
                datePickerDialog.getDatePicker().setCalendarViewShown(false);
                datePickerDialog.show();
            }
        };
        startDate.setOnClickListener(showDatePicker);
        endDate.setOnClickListener(showDatePicker);
        makeQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bundle bundle = new Bundle();
                bundle.putString("startDate", String.valueOf(mStartDateValue.getTime() / 1000));
                bundle.putString("endDate", String.valueOf(mEndDateValue.getTime() / 1000));
                getLoaderManager().restartLoader(mLoaderId, bundle, HistoryFragment.this);
                mListView.setAdapter(null);
                mNoItems.setVisibility(View.GONE);
                mListView.setEmptyView(mLoadingView);

            }
        });
        mLoadingView = (ProgressBar) getView().findViewById(R.id.Loading);
        mNoItems = (TextView) getView().findViewById(R.id.NoItems);
        mListView.setEmptyView(mLoadingView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_refresh_filter, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("startDate", mDateFormat.format(mStartDateValue));
        outState.putString("endDate", mDateFormat.format(mEndDateValue));
    }

    @Override
    public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
        if (mNoItems != null) {
            mNoItems.setVisibility(View.GONE);
        }
        if (mLoadingView != null) {
            mListView.setEmptyView(mLoadingView);
        }
        return (new OrdersLoader(getActivity(), args, mHistoryType));
    }

    @Override
    public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
        if (data == null) {
            Toast.makeText(getActivity(), getResources().getString(R.string.GeneralErrorText), Toast.LENGTH_LONG)
                    .show();
            mNoItems.setText(getResources().getString(R.string.OoopsError).toUpperCase(Locale.US));
            mListView.setEmptyView(mNoItems);
            mLoadingView.setVisibility(View.GONE);
        } else if (data.optInt("success") == 0) {
            mNoItems.setText(data.optString("error").toUpperCase(Locale.US));
            mListView.setEmptyView(mNoItems);
            mLoadingView.setVisibility(View.GONE);
        } else {
            if (mHistoryType == ListType.Trades) {
                try {
                    long start = mStartDateValue.getTime() / 1000L;
                    JSONObject out = new JSONObject(); //workaround for TradeHistory bug
                    out.put("success", 1);
                    JSONObject returnObject = new JSONObject();
                    int count = 0;
                    for (@SuppressWarnings("unchecked") Iterator<String> iterator = data.optJSONObject("return").keys();
                         iterator.hasNext(); ) {
                        String key = iterator.next();
                        if (data.optJSONObject("return").optJSONObject(key).optLong("timestamp") > start) {
                            returnObject.put(key, data.optJSONObject("return").optJSONObject(key));
                            count++;
                        }
                    }
                    out.put("return", returnObject);
                    if (count == 0) {
                        mNoItems.setText("No items".toUpperCase(Locale.US));
                        mListView.setEmptyView(mNoItems);
                        mLoadingView.setVisibility(View.GONE);
                    } else {
                        mAdapter.updateEntries(out);
                        mListView.setAdapter(mAdapter);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                mAdapter.updateEntries(data);
                mListView.setAdapter(mAdapter);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<JSONObject> loader) {
        mListView.setAdapter(null);
        mNoItems.setVisibility(View.GONE);
        mListView.setEmptyView(mLoadingView);
    }
}
