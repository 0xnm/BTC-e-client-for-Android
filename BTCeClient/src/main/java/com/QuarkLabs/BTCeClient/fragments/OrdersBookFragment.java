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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.OrdersBookAdapter;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.Depth;
import com.QuarkLabs.BTCeClient.api.PriceVolumePair;
import com.QuarkLabs.BTCeClient.loaders.OrderBookLoader;

import org.stockchart.StockChartView;
import org.stockchart.core.Axis;
import org.stockchart.series.LinearSeries;

import java.util.ArrayList;
import java.util.List;

public class OrdersBookFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<CallResult<Depth>> {
    private static final int LOADER_ID = 1;

    private static final String POSITION_KEY = "position";

    private ListView mAsksList;
    private ListView mBidsList;
    private FrameLayout mChartArea;

    private OrdersBookAdapter mAsksAdapter;
    private OrdersBookAdapter mBidsAdapter;
    private Spinner pairsSpinner;

    private ProgressBar mLoadingViewAsks;
    private ProgressBar mLoadingViewBids;

    private boolean isFragmentOpenedFirstTime = true;
    private int spinnerPosition = -1;

    private AppPreferences appPreferences;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity hostActivity = (AppCompatActivity) getActivity();

        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();
        Context themedContext = hostActivity.getSupportActionBar()
                .getThemedContext();
        pairsSpinner = (Spinner) LayoutInflater.from(themedContext)
                .inflate(R.layout.spinner, null);
        List<String> pairs = new ArrayList<>(appPreferences.getExchangePairs());
        ArrayAdapter<String> pairsAdapter = new ArrayAdapter<>(
                new ContextThemeWrapper(themedContext, R.style.ThemeOverlay_AppCompat_Light),
                android.R.layout.simple_spinner_item, pairs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            pairsSpinner.setDropDownWidth((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics()));
        }
        pairsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        pairsSpinner.setAdapter(pairsAdapter);

        //restoring spinner position
        if (spinnerPosition != -1) {
            pairsSpinner.setSelection(spinnerPosition);
        } else if (savedInstanceState != null) {
            spinnerPosition = savedInstanceState.getInt(POSITION_KEY);
            pairsSpinner.setSelection(spinnerPosition);
        }

        pairsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getSelectedItem().toString();
                Bundle bundle = new Bundle();
                bundle.putString("pair", selected);
                if (isFragmentOpenedFirstTime) {
                    getLoaderManager().initLoader(LOADER_ID, bundle, OrdersBookFragment.this);
                    isFragmentOpenedFirstTime = false;
                } else {
                    getLoaderManager().restartLoader(LOADER_ID, bundle, OrdersBookFragment.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAsksAdapter = new OrdersBookAdapter();
        mBidsAdapter = new OrdersBookAdapter();

        hostActivity.getSupportActionBar()
                .setCustomView(pairsSpinner, new ActionBar.LayoutParams(Gravity.END));
        hostActivity.getSupportActionBar().setDisplayShowCustomEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_ordersbook, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        mAsksList = (ListView) view.findViewById(R.id.asks);
        mBidsList = (ListView) view.findViewById(R.id.bids);
        mChartArea = (FrameLayout) view.findViewById(R.id.OrdersBookChart);
        mLoadingViewAsks = new ProgressBar(getActivity());
        mLoadingViewBids = new ProgressBar(getActivity());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        lp.gravity = Gravity.CENTER;
        mLoadingViewAsks.setLayoutParams(lp);

        mLoadingViewBids.setLayoutParams(lp);
        ((LinearLayout) mAsksList.getParent()).addView(mLoadingViewAsks);
        mAsksList.setEmptyView(mLoadingViewAsks);
        ((LinearLayout) mBidsList.getParent()).addView(mLoadingViewBids);
        mBidsList.setEmptyView(mLoadingViewBids);
    }

    @Override
    public Loader<CallResult<Depth>> onCreateLoader(int id, Bundle args) {
        mAsksList.setAdapter(null);
        mBidsList.setAdapter(null);
        return new OrderBookLoader(getActivity(), args.getString("pair"));
    }

    @Override
    public void onLoadFinished(Loader<CallResult<Depth>> loader, CallResult<Depth> result) {
        if (!result.isSuccess()) {
            Toast.makeText(getActivity(), R.string.general_error_text, Toast.LENGTH_LONG).show();
        } else {
            final List<PriceVolumePair> asks = result.getPayload().getAsks();
            final List<PriceVolumePair> bids = result.getPayload().getBids();

            mAsksAdapter.pushData(asks);
            mBidsAdapter.pushData(bids);
            mAsksList.setAdapter(mAsksAdapter);
            mBidsList.setAdapter(mBidsAdapter);
            mChartArea.removeAllViews();
            StockChartView chartView = new StockChartView(getActivity());
            final LinearSeries asksSeries = new LinearSeries();
            final LinearSeries bidsSeries = new LinearSeries();
            asksSeries.getAppearance().setOutlineColor(0xffff4444);
            bidsSeries.getAppearance().setOutlineColor(0xff0099cc);
            double sumAsks = 0.0;
            double sumBids = 0.0;
            for (int i = 0; i < bids.size(); i++) {
                sumBids += bids.get(i).getVolume().doubleValue();
            }
            for (int i = bids.size() - 1; i >= 0; i--) {
                sumBids -= bids.get(i).getVolume().doubleValue();
                bidsSeries.addPoint(sumBids);
            }
            for (int i = 0; i < asks.size(); i++) {
                asksSeries.addPoint(sumAsks);
                sumAsks += asks.get(i).getVolume().doubleValue();
            }

            asksSeries.setIndexOffset(bidsSeries.getPointCount());

            chartView.addArea().getSeries().add(asksSeries);
            chartView.getAreas().get(0).getSeries().add(bidsSeries);

            Axis.ILabelFormatProvider provider = new Axis.ILabelFormatProvider() {
                @Override
                public String getAxisLabel(Axis axis, double v) {
                    int index = bidsSeries.convertToArrayIndex(v);
                    if (index < 0) {
                        index = 0;
                    }
                    if (index >= 0) {
                        if (index >= bidsSeries.getPointCount()) {
                            index = asksSeries.convertToArrayIndex(v);
                            if (index < 0) {
                                index = 0;
                            }
                            if (index >= 0) {
                                if (index >= asksSeries.getPointCount()) {
                                    index = asksSeries.getPointCount() - 1;
                                }
                            }
                            return asks.get(index).getPrice().toPlainString();
                        }
                        return bids.get(bidsSeries.getPointCount() - 1 - index)
                                .getPrice().toPlainString();
                    }
                    return null;

                }
            };
            chartView.getAreas().get(0).getBottomAxis().setLabelFormatProvider(provider);


            //customizing fonts for chart
            chartView.getAreas().get(0).setTitle("Market Depth for "
                    + pairsSpinner.getSelectedItem().toString()
                    + " (Price vs. Volume)");
            chartView.getAreas().get(0).getPlot()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getLeftAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getTopAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getBottomAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getRightAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getBottomAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                            getResources().getDisplayMetrics()));
            chartView.getAreas().get(0)
                    .getRightAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                            getResources().getDisplayMetrics()));
            mChartArea.addView(chartView);
            chartView.invalidate();
        }

    }

    @Override
    public void onLoaderReset(Loader<CallResult<Depth>> loader) {
        mAsksList.setAdapter(null);
        mBidsList.setAdapter(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_menu_ordersbook, menu);
    }

    @Override
    public void onDestroyView() {
        AppCompatActivity hostActivity = (AppCompatActivity) getActivity();
        hostActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        spinnerPosition = pairsSpinner.getSelectedItemPosition();
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Bundle bundle = new Bundle();
                bundle.putString("pair", pairsSpinner.getSelectedItem().toString());
                getLoaderManager().restartLoader(LOADER_ID, bundle, this);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //saving position of spinner
        // don't know how, but NPE can happen
        if (pairsSpinner != null) {
            outState.putInt(POSITION_KEY, pairsSpinner.getSelectedItemPosition());
        }
    }
}
