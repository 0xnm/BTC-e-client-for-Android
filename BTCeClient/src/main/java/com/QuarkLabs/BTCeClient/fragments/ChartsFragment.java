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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.CheckBoxListAdapter;
import org.stockchart.StockChartView;
import org.stockchart.core.*;
import org.stockchart.indicators.*;
import org.stockchart.series.StockSeries;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartsFragment extends Fragment {
    private Map<String, View> mCharts;
    private View mRootView;
    private LayoutInflater mInflater;
    private List<String> mCookies = Collections.synchronizedList(new ArrayList<String>());
    private ChartsUpdater mChartsUpdater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mInflater = inflater;
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_chart, container, false);
        }
        mChartsUpdater = new ChartsUpdater(new Handler());
        mChartsUpdater.setListener(new Listener<StockChartView>() {
            @Override
            public void onChartDownloaded(StockChartView stockChartView) {
                if (isVisible()) {
                    stockChartView.invalidate();
                }
            }
        });

        mChartsUpdater.start();
        mChartsUpdater.getLooper();
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        refreshChartViews(mInflater);
        updateCharts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mChartsUpdater.clearQueue();
        mChartsUpdater.quit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                updateCharts();
                break;
            case R.id.action_add:
                final CheckBoxListAdapter checkBoxListAdapter = new CheckBoxListAdapter(getActivity(),
                        getResources().getStringArray(R.array.ExchangePairs),
                        CheckBoxListAdapter.SettingsScope.CHARTS);
                ListView v = new ListView(getActivity());
                v.setAdapter(checkBoxListAdapter);
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.SelectPairsPromptTitle))
                        .setView(v)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkBoxListAdapter.saveValuesToPreferences();
                                refreshChartViews(mInflater);
                                updateCharts();
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_add_refresh, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Starts fetching charts data via AsyncTasks
     */
    private void updateCharts() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getActivity(), "No connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mCharts.size() > 0) {
            Toast.makeText(getActivity(), "Updating charts", Toast.LENGTH_SHORT).show();
            Set<String> chartNames = mCharts.keySet();
            String[] chartsNamesSorted = chartNames.toArray(new String[chartNames.size()]);
            Arrays.sort(chartsNamesSorted);

            for (String x : chartsNamesSorted) {
                String pair = x.replace("/", "_").toLowerCase(Locale.US);
                mChartsUpdater.queueChart((StockChartView) mCharts.get(x).findViewById(R.id.StockChartView),
                        pair);
            }
        }
    }

    /**
     * Creates views for charts
     *
     * @param inflater Inflater to be used for creating views
     */

    private void refreshChartViews(LayoutInflater inflater) {

        LinearLayout chartsContainer = (LinearLayout) mRootView.findViewById(R.id.ChartsContainer);
        chartsContainer.removeAllViews();

        mCharts = new HashMap<>();
        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Set<String> hashSet = sh.getStringSet("ChartsToDisplay", new HashSet<String>());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;

        TextView noCharts = new TextView(getActivity());
        noCharts.setLayoutParams(lp);
        noCharts.setGravity(Gravity.CENTER);
        noCharts.setText("NO CHARTS");
        noCharts.setTypeface(Typeface.DEFAULT_BOLD);
        //if no pairs to display found in prefs, display "NO CHARTS" text
        if (hashSet.size() == 0) {
            chartsContainer.addView(noCharts);
        }
        String[] pairs = hashSet.toArray(new String[hashSet.size()]);
        Arrays.sort(pairs);

        for (String x : pairs) {
            View element = inflater.inflate(R.layout.chart_item, chartsContainer, false);
            chartsContainer.addView(element);
            mCharts.put(x, element);
        }

        CompoundButton.OnCheckedChangeListener IndicatorListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ViewGroup viewGroup;
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    viewGroup = (ViewGroup) buttonView.getParent().getParent().getParent();
                } else {
                    viewGroup = (ViewGroup) buttonView.getParent();
                }
                StockChartView stockChartView = (StockChartView) viewGroup.findViewById(R.id.StockChartView);
                IndicatorManager iManager = stockChartView.getIndicatorManager();
                if (stockChartView.findSeriesByName("price") != null) {
                    if (isChecked) {
                        //if switch turned on and chart has data
                        switch (buttonView.getId()) {
                            case R.id.enableEMAIndicator:
                                iManager.addEma(stockChartView.findSeriesByName("price"), 0);
                                break;
                            case R.id.enableMACDIndicator:
                                iManager.addMacd(stockChartView.findSeriesByName("price"), 0);
                                break;
                            case R.id.enableRSIIndicator:
                                iManager.addRsi(stockChartView.findSeriesByName("price"), 0);
                                break;
                            case R.id.enableSMAIndicator:
                                iManager.addSma(stockChartView.findSeriesByName("price"), 0);
                                break;
                            default:
                                break;
                        }
                        stockChartView.recalcIndicators();
                        stockChartView.recalc();
                        stockChartView.invalidate();
                    } else {
                        //if switch turned off and chart has no data
                        List<AbstractIndicator> indicators = stockChartView.getIndicatorManager().getIndicators();
                        for (AbstractIndicator x : indicators) {
                            switch (buttonView.getId()) {
                                case R.id.enableEMAIndicator:
                                    if (x instanceof EmaIndicator) {
                                        stockChartView.getIndicatorManager().removeIndicator(x);
                                    }
                                    break;
                                case R.id.enableMACDIndicator:
                                    if (x instanceof MacdIndicator) {
                                        stockChartView.getIndicatorManager().removeIndicator(x);
                                    }
                                    break;
                                case R.id.enableRSIIndicator:
                                    if (x instanceof RsiIndicator) {
                                        stockChartView.getIndicatorManager().removeIndicator(x);
                                    }
                                    break;
                                case R.id.enableSMAIndicator:
                                    if (x instanceof SmaIndicator) {
                                        stockChartView.getIndicatorManager().removeIndicator(x);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        stockChartView.recalc();
                        stockChartView.invalidate();
                    }
                } else {
                    //if chart has no data, ignore switch
                    Switch sw = (Switch) buttonView;
                    sw.setChecked(!isChecked);
                }
            }
        };

        //add listeners to switches
        for (String x : mCharts.keySet()) {
            ((Switch) mCharts.get(x).findViewById(R.id.enableEMAIndicator))
                    .setOnCheckedChangeListener(IndicatorListener);
            ((Switch) mCharts.get(x).findViewById(R.id.enableRSIIndicator))
                    .setOnCheckedChangeListener(IndicatorListener);
            ((Switch) mCharts.get(x).findViewById(R.id.enableSMAIndicator))
                    .setOnCheckedChangeListener(IndicatorListener);
            ((Switch) mCharts.get(x).findViewById(R.id.enableMACDIndicator))
                    .setOnCheckedChangeListener(IndicatorListener);
        }
    }

    /**
     * Shows error, helper function
     */
    private void showError() {
        final String errorText = getResources().getString(R.string.GeneralErrorText);
        if (isVisible() && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), errorText, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static interface Listener<StockChartView> {
        void onChartDownloaded(StockChartView token);
    }

    private class ChartsUpdater extends HandlerThread {

        private static final int MESSAGE_DOWNLOAD = 0;
        private static final String TAG = "ChartDownloaderThread";
        private Handler mHandler;
        private Handler mResponseHandler;
        private Map<StockChartView, String> requestMap = Collections.synchronizedMap(new HashMap<StockChartView,
                String>());
        private Listener<StockChartView> mListener;


        public ChartsUpdater(Handler responseHandler) {
            super(TAG);
            mResponseHandler = responseHandler;
        }

        @SuppressLint("HandlerLeak")
        @Override
        protected void onLooperPrepared() {
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_DOWNLOAD) {
                        @SuppressWarnings("unchecked")
                        StockChartView token = (StockChartView) msg.obj;
                        handleRequest(token);
                    }
                }
            };
        }

        private void handleRequest(final StockChartView token) {
            final String pair = requestMap.get(token);
            if (pair == null) {
                return;
            }
            ChartDataDownloader chartDataDownloader = new ChartDataDownloader();
            if (mCookies.size() == 0) {
                chartDataDownloader.getCookies();
            }
            String[] data = chartDataDownloader.getChartData(pair);
            if (data != null && requestMap.get(token) != null) {
                updateChart(token, data);
            }
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    requestMap.remove(token);
                    mListener.onChartDownloaded(token);
                }
            });
        }

        public void setListener(Listener<StockChartView> listener) {
            mListener = listener;
        }

        public void queueChart(StockChartView token, String pair) {
            requestMap.put(token, pair);
            mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
        }

        public void clearQueue() {
            mHandler.removeMessages(MESSAGE_DOWNLOAD);
            requestMap.clear();
        }

        private void updateChart(StockChartView token, final String[] data) {

            final StockSeries fPriceSeries = new StockSeries();
            fPriceSeries.setViewType(StockSeries.ViewType.CANDLESTICK);
            fPriceSeries.setName("price");
            token.reset();
            token.getCrosshair().setAuto(true);
            Area area = token.addArea();
            area.getLegend().getAppearance().getFont().setSize(16);
            String pair = requestMap.get(token);
            area.setTitle(pair.replace("_", "/").toUpperCase(Locale.US));

            for (String x : data) {
                String[] values = x.split(", ");
                fPriceSeries.addPoint(Double.parseDouble(values[2]),
                        Double.parseDouble(values[4]),
                        Double.parseDouble(values[1]),
                        Double.parseDouble(values[3]));
            }
            area.getSeries().add(fPriceSeries);
            //provider for bottom axis (time)
            Axis.ILabelFormatProvider bottomLfp = new Axis.ILabelFormatProvider() {
                @Override
                public String getAxisLabel(Axis axis, double v) {
                    int index = fPriceSeries.convertToArrayIndex(v);
                    if (index < 0)
                        index = 0;
                    if (index >= 0) {
                        if (index >= fPriceSeries.getPointCount())
                            index = fPriceSeries.getPointCount() - 1;

                        return data[index].split(", ")[0].replace("\"", "");
                    }
                    return null;
                }
            };
            //provider for crosshair
            Crosshair.ILabelFormatProvider dp = new Crosshair.ILabelFormatProvider() {
                @Override
                public String getLabel(Crosshair crosshair, Plot plot, double v, double v2) {
                    int index = fPriceSeries.convertToArrayIndex(v);
                    if (index < 0)
                        index = 0;
                    if (index >= 0) {
                        if (index >= fPriceSeries.getPointCount())
                            index = fPriceSeries.getPointCount() - 1;

                        return data[index].split(", ")[0].replace("\"", "")
                                + " - "
                                + (new DecimalFormat("#.#####")
                                .format(v2));
                    }
                    return null;
                }
            };

            token.getCrosshair().setLabelFormatProvider(dp);
            //provider for right axis (value)
            Axis.ILabelFormatProvider rightLfp = new Axis.ILabelFormatProvider() {
                @Override
                public String getAxisLabel(Axis axis, double v) {
                    DecimalFormat decimalFormat = new DecimalFormat("#.#######");
                    return decimalFormat.format(v);
                }
            };
            area.getRightAxis().setLabelFormatProvider(rightLfp);
            area.getBottomAxis().setLabelFormatProvider(bottomLfp);

            //by some reason rise and fall appearance should be switched
            Appearance riseAppearance = fPriceSeries.getRiseAppearance();
            Appearance riseAppearance1 = new Appearance();
            riseAppearance1.fill(riseAppearance);
            Appearance fallAppearance = fPriceSeries.getFallAppearance();
            riseAppearance.fill(fallAppearance);
            fallAppearance.fill(riseAppearance1);

            //styling: setting fonts
            area.getPlot()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                            getResources().getDisplayMetrics()));
            area.getLeftAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                            getResources().getDisplayMetrics()));
            area.getTopAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                            getResources().getDisplayMetrics()));
            area.getBottomAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                            getResources().getDisplayMetrics()));
            area.getRightAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                            getResources().getDisplayMetrics()));
            area.getBottomAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                            getResources().getDisplayMetrics()));
            area.getRightAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                            getResources().getDisplayMetrics()));
            token.getCrosshair().getAppearance().getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                            getResources().getDisplayMetrics()));
        }
    }

    private class ChartDataDownloader {

        private static final String mBaseUrl = "https://btc-e.com/exchange/";

        String[] getChartData(String pair) {

            StringBuilder out = new StringBuilder();
            try {
                URL url = new URL(mBaseUrl + pair);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                for (String cookie : mCookies) {
                    connection.addRequestProperty("Cookie", cookie);
                }
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    showError();
                    return null;
                }
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    out.append(line);
                }
                Pattern pattern = Pattern.compile("ToDataTable\\(\\[\\[(.+?)\\]\\], true");
                Matcher matcher = pattern.matcher(out.toString());
                if (matcher.find()) {
                    //chart data
                    return matcher.group(1).split("\\],\\[");
                } else {
                    //if data not found, maybe cookie was outdated
                    mCookies.clear();
                    showError();
                }
                rd.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        void getCookies() {
            try {
                URL url = new URL("https://btc-e.com/");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    showError();
                    return;
                }

                mCookies.addAll(connection.getHeaderFields().get("Set-Cookie"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                Pattern pattern = Pattern.compile("document.cookie=\"(a=(.+?));");
                Matcher matcher = pattern.matcher(reader.readLine());

                //if cookie not found
                if (!matcher.find()) {
                    showError();
                    return;
                }
                reader.close();
                connection.disconnect();
                mCookies.add(matcher.group(1));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


