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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.OrdersBookAdapter;
import com.QuarkLabs.BTCeClient.loaders.OrderBookLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.stockchart.StockChartView;
import org.stockchart.core.Axis;
import org.stockchart.series.LinearSeries;

public class OrdersBookFragment extends Fragment implements LoaderManager.LoaderCallbacks<JSONObject> {
    private static final int LOADER_ID = 1;
    private ListView mAsksList;
    private ListView mBidsList;
    private FrameLayout mChartArea;
    private OrdersBookAdapter mAsksAdapter;
    private OrdersBookAdapter mBidsAdapter;
    private Spinner mPairsSpinner;
    private ProgressBar mLoadingViewAsks;
    private ProgressBar mLoadingViewBids;
    private boolean mFragmentOpenedFirstTime = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoadingViewAsks = new ProgressBar(getActivity());
        mLoadingViewBids = new ProgressBar(getActivity());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        mLoadingViewAsks.setLayoutParams(lp);

        mLoadingViewBids.setLayoutParams(lp);
        mPairsSpinner = new Spinner(getActivity());
        mPairsSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.ExchangePairs)));
        //restoring spinner position
        if (savedInstanceState != null) {
            mPairsSpinner.setSelection(savedInstanceState.getInt("position"));
        }
        mPairsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getSelectedItem().toString();
                Bundle bundle = new Bundle();
                bundle.putString("pair", selected);
                if (mFragmentOpenedFirstTime) {
                    getLoaderManager().initLoader(LOADER_ID, bundle, OrdersBookFragment.this);
                    mFragmentOpenedFirstTime = false;
                } else {
                    getLoaderManager().restartLoader(LOADER_ID, bundle, OrdersBookFragment.this);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAsksAdapter = new OrdersBookAdapter(getActivity());
        mBidsAdapter = new OrdersBookAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.fragment_ordersbook, null, false);
        mAsksList = (ListView) v.findViewById(R.id.asks);
        mBidsList = (ListView) v.findViewById(R.id.bids);
        mChartArea = (FrameLayout) v.findViewById(R.id.OrdersBookChart);
        ((LinearLayout) mAsksList.getParent()).addView(mLoadingViewAsks);

        mAsksList.setEmptyView(mLoadingViewAsks);
        ((LinearLayout) mBidsList.getParent()).addView(mLoadingViewBids);
        mBidsList.setEmptyView(mLoadingViewBids);

        getActivity().getActionBar().setCustomView(mPairsSpinner, new ActionBar.LayoutParams(Gravity.RIGHT));
        getActivity().getActionBar().setDisplayShowCustomEnabled(true);
        return v;
    }

    @Override
    public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
        mAsksList.setAdapter(null);
        mBidsList.setAdapter(null);
        return new OrderBookLoader(getActivity(), args.getString("pair"));
    }

    @Override
    public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
        if (data == null) {
            Toast.makeText(getActivity(), R.string.GeneralErrorText, Toast.LENGTH_LONG).show();
        } else if (data.length() == 0) {
            Toast.makeText(getActivity(), R.string.GeneralErrorText, Toast.LENGTH_LONG).show();
        } else {
            final JSONArray asks = data.optJSONArray("asks");
            final JSONArray bids = data.optJSONArray("bids");
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
            for (int i = 0; i < bids.length(); i++) {
                sumBids += bids.optJSONArray(i).optDouble(1);
            }
            for (int i = bids.length() - 1; i >= 0; i--) {
                sumBids -= bids.optJSONArray(i).optDouble(1);
                bidsSeries.addPoint(sumBids);
            }
            for (int i = 0; i < asks.length(); i++) {

                asksSeries.addPoint(sumAsks);
                sumAsks += asks.optJSONArray(i).optDouble(1);
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
                            return asks.optJSONArray(index).optString(0);
                        }
                        return bids.optJSONArray(bidsSeries.getPointCount() - 1 - index).optString(0);
                    }
                    return null;

                }
            };
            chartView.getAreas().get(0).getBottomAxis().setLabelFormatProvider(provider);


            //customizing fonts for chart
            chartView.getAreas().get(0).setTitle("Market Depth for "
                    + mPairsSpinner.getSelectedItem().toString()
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
    public void onLoaderReset(Loader<JSONObject> loader) {
        mAsksList.setAdapter(null);
        mBidsList.setAdapter(null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_menu_ordersbook, menu);
    }

    @Override
    public void onDestroyView() {
        getActivity().getActionBar().setDisplayShowCustomEnabled(false);
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Bundle bundle = new Bundle();
                bundle.putString("pair", mPairsSpinner.getSelectedItem().toString());
                getLoaderManager().restartLoader(LOADER_ID, bundle, this);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //saving position of spinner
        outState.putInt("position", mPairsSpinner.getSelectedItemPosition());
    }
}
