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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter;

import org.stockchart.StockChartView;
import org.stockchart.core.Appearance;
import org.stockchart.core.Area;
import org.stockchart.core.Axis;
import org.stockchart.core.Crosshair;
import org.stockchart.core.IndicatorManager;
import org.stockchart.core.Plot;
import org.stockchart.indicators.AbstractIndicator;
import org.stockchart.indicators.EmaIndicator;
import org.stockchart.indicators.MacdIndicator;
import org.stockchart.indicators.RsiIndicator;
import org.stockchart.indicators.SmaIndicator;
import org.stockchart.series.StockSeries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class ChartsFragment extends Fragment {

    private ChartsDelegate chartsDelegate;
    private boolean isUpdating;
    private View rootView;
    private AlertDialog chartsDialog;
    private AppPreferences appPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_chart, container, false);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();
        boolean useOldCharts = BtcEApplication.get(getActivity())
                .getAppPreferences().isShowOldCharts();
        chartsDelegate = new BtceChartsDelegate();
        chartsDelegate.onViewCreated();
        chartsDelegate.createChartViews();
        chartsDelegate.updateChartData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                chartsDelegate.updateChartData();
                break;
            case R.id.action_add:
                final PairsCheckboxAdapter pairsCheckboxAdapter =
                        new PairsCheckboxAdapter(getActivity(),
                                appPreferences.getExchangePairs(),
                                PairsCheckboxAdapter.SettingsScope.CHARTS);
                ListView v = new ListView(getActivity());
                v.setAdapter(pairsCheckboxAdapter);
                chartsDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.SelectPairsPromptTitle)
                        .setView(v)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                pairsCheckboxAdapter.saveValuesToPreferences();
                                chartsDelegate.createChartViews();
                                chartsDelegate.updateChartData();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chartsDialog != null) {
            chartsDialog.dismiss();
            chartsDialog = null;
        }
        chartsDelegate.onDestroyView();
        chartsDelegate = null;
    }

    /**
     * Shows error, helper function
     */
    private void showError() {
        if (isVisible() && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(),
                            R.string.general_error_text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (isUpdating) {
            refreshItem.setActionView(R.layout.progress_bar_action_view);
            refreshItem.expandActionView();
        } else {
            refreshItem.collapseActionView();
            refreshItem.setActionView(null);
        }
        super.onPrepareOptionsMenu(menu);
    }

    private interface ChartsDelegate {
        void onViewCreated();

        void onDestroyView();

        void createChartViews();

        void updateChartData();
    }

    private class TradingViewChartsDelegate implements ChartsDelegate {


        @Override
        public void onViewCreated() {
            // no-op
        }

        @Override
        public void onDestroyView() {
            // no-op
        }

        @Override
        public void createChartViews() {
            LinearLayout chartsContainer = (LinearLayout)
                    getView().findViewById(R.id.ChartsContainer);
            chartsContainer.removeAllViews();

            Set<String> pairsSet = new HashSet<>(
                    PairUtils.getChartsToDisplayThatSupported(getActivity()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            lp.gravity = Gravity.CENTER;

            TextView noCharts = new TextView(getActivity());
            noCharts.setLayoutParams(lp);
            noCharts.setGravity(Gravity.CENTER);
            noCharts.setText(R.string.no_charts_text);
            noCharts.setTypeface(Typeface.DEFAULT_BOLD);
            //if no pairs to display found in prefs, display "NO CHARTS" text
            if (pairsSet.size() == 0) {
                chartsContainer.addView(noCharts);
            }
            String[] pairs = pairsSet.toArray(new String[pairsSet.size()]);
            Arrays.sort(pairs);

            for (String pair : pairs) {
                WebView chartView = new WebView(getActivity());
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350,
                                getResources().getDisplayMetrics())
                );
                clp.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                        getResources().getDisplayMetrics());
                chartView.setLayoutParams(clp);
                chartView.getSettings().setDomStorageEnabled(true);
                chartView.getSettings().setJavaScriptEnabled(true);
                chartView.loadData(getChartHtml(toTradingViewSymbol(pair)), "text/html", "UTF-8");
                chartsContainer.addView(chartView);
            }
        }

        @Override
        public void updateChartData() {
            // no-op, data is fetched through widget
        }

        @NonNull
        private String toTradingViewSymbol(@NonNull String pair) {
            return "BTCE:" + pair.toUpperCase(Locale.US).replace("/", "");
        }

        @NonNull
        private String getChartHtml(@NonNull String tradingViewSymbol) {
            return String.format("<html>\n" +
                    "<body>\n" +
                    "<script type='text/javascript' src='https://d33t3vvu2t2yu5.cloudfront.net/tv.js'></script>\n" +
                    "<script type='text/javascript'>\n" +
                    "\tnew TradingView.widget({\n" +
                    "\t  'autosize': true,\n" +
                    "\t  'symbol': '%s',\n" +
                    "\t  'interval': '15',\n" +
                    "\t  'timezone': 'Europe/Moscow',\n" +
                    "\t  'theme': 'White',\n" +
                    "\t  'style': '1',\n" +
                    "\t  'locale': 'en',\n" +
                    "\t  'toolbar_bg': '#f1f3f6',\n" +
                    "\t  'enable_publishing': false,\n" +
                    "\t  'allow_symbol_change': false,\n" +
                    "\t  'save_image': false,\n" +
                    "\t  'hideideas': true,\n" +
                    "\t  'withdateranges': true,\n" +
                    "\t  'whitelabel': true,\n" +
                    "\t});\n" +
                    "\t</script>\n" +
                    "</body>\n" +
                    "</html>", tradingViewSymbol);
        }
    }

    private class BtceChartsDelegate implements ChartsDelegate {

        private Map<String, View> chartsMap;
        private ChartsUpdater chartsUpdater;
        private Handler responseHandler;

        @Override
        public void onViewCreated() {
            responseHandler = new Handler();
            chartsUpdater = new ChartsUpdater(responseHandler);
            chartsUpdater.setListener(new Listener<StockChartView>() {
                @Override
                public void onChartDownloaded(@NonNull StockChartView view,
                                              @NonNull String pair, @NonNull final String[] data) {

                    final StockSeries fPriceSeries = new StockSeries();
                    fPriceSeries.setViewType(StockSeries.ViewType.CANDLESTICK);
                    fPriceSeries.setName("price");
                    view.reset();
                    view.getCrosshair().setAuto(true);
                    Area area = view.addArea();
                    area.getLegend().getAppearance().getFont().setSize(16);
                    area.setTitle(PairUtils.serverToLocal(pair));

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

                    view.getCrosshair().setLabelFormatProvider(dp);
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

                    Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    final Resources resources = activity.getResources();

                    //styling: setting fonts
                    area.getPlot()
                            .getAppearance()
                            .getFont()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                                    resources.getDisplayMetrics()));
                    area.getLeftAxis()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                                    resources.getDisplayMetrics()));
                    area.getTopAxis()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                                    resources.getDisplayMetrics()));
                    area.getBottomAxis()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                                    resources.getDisplayMetrics()));
                    area.getRightAxis()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                                    resources.getDisplayMetrics()));
                    area.getBottomAxis()
                            .getAppearance()
                            .getFont()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                                    resources.getDisplayMetrics()));
                    area.getRightAxis()
                            .getAppearance()
                            .getFont()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9,
                                    resources.getDisplayMetrics()));
                    view.getCrosshair().getAppearance().getFont()
                            .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                                    resources.getDisplayMetrics()));
                    view.invalidate();
                }
            });

            chartsUpdater.start();
            chartsUpdater.createHandler();
        }

        @Override
        public void onDestroyView() {
            chartsUpdater.clearQueue();
            chartsUpdater.quit();
        }

        @Override
        public void createChartViews() {
            LinearLayout chartsContainer = (LinearLayout) getView()
                    .findViewById(R.id.ChartsContainer);
            chartsContainer.removeAllViews();

            chartsMap = new HashMap<>();
            Set<String> pairsSet = new HashSet<>(
                    PairUtils.getChartsToDisplayThatSupported(getActivity()));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            lp.gravity = Gravity.CENTER;

            TextView noCharts = new TextView(getActivity());
            noCharts.setLayoutParams(lp);
            noCharts.setGravity(Gravity.CENTER);
            noCharts.setText(getString(R.string.no_charts_text));
            noCharts.setTypeface(Typeface.DEFAULT_BOLD);
            //if no pairs to display found in prefs, display "NO CHARTS" text
            if (pairsSet.isEmpty()) {
                chartsContainer.addView(noCharts);
            }
            String[] pairs = pairsSet.toArray(new String[pairsSet.size()]);
            Arrays.sort(pairs);

            for (String x : pairs) {
                View element = LayoutInflater.from(getActivity())
                        .inflate(R.layout.chart_item, chartsContainer, false);
                chartsContainer.addView(element);
                chartsMap.put(x, element);
            }

            CompoundButton.OnCheckedChangeListener indicatorChangeStateListener
                    = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ViewGroup viewGroup;
                    if (getResources().getConfiguration().screenWidthDp >= 350) {
                        viewGroup = (ViewGroup) buttonView.getParent().getParent().getParent();
                    } else {
                        viewGroup = (ViewGroup) buttonView.getParent();
                    }
                    StockChartView stockChartView
                            = (StockChartView) viewGroup.findViewById(R.id.stockChartView);
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
                            Iterator<AbstractIndicator> iterator = stockChartView
                                    .getIndicatorManager()
                                    .getIndicators()
                                    .iterator();
                            while (iterator.hasNext()) {
                                AbstractIndicator x = iterator.next();
                                switch (buttonView.getId()) {
                                    case R.id.enableEMAIndicator:
                                        if (x instanceof EmaIndicator) {
                                            iterator.remove();
                                            stockChartView.getIndicatorManager().removeIndicator(x);
                                        }
                                        break;
                                    case R.id.enableMACDIndicator:
                                        if (x instanceof MacdIndicator) {
                                            iterator.remove();
                                            stockChartView.getIndicatorManager().removeIndicator(x);
                                        }
                                        break;
                                    case R.id.enableRSIIndicator:
                                        if (x instanceof RsiIndicator) {
                                            iterator.remove();
                                            stockChartView.getIndicatorManager().removeIndicator(x);
                                        }
                                        break;
                                    case R.id.enableSMAIndicator:
                                        if (x instanceof SmaIndicator) {
                                            iterator.remove();
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
                        SwitchCompat sw = (SwitchCompat) buttonView;
                        sw.setChecked(!isChecked);
                    }
                }
            };

            //add listeners to switches
            for (String x : chartsMap.keySet()) {
                ((SwitchCompat) chartsMap.get(x).findViewById(R.id.enableEMAIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartsMap.get(x).findViewById(R.id.enableRSIIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartsMap.get(x).findViewById(R.id.enableSMAIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartsMap.get(x).findViewById(R.id.enableMACDIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
            }
        }

        @Override
        public void updateChartData() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo == null || !networkInfo.isConnected()) {
                Toast.makeText(getActivity(), R.string.no_connection_text, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (chartsMap.size() > 0) {
                Set<String> chartNames = chartsMap.keySet();
                String[] chartsNamesSorted = chartNames.toArray(new String[chartNames.size()]);
                Arrays.sort(chartsNamesSorted);
                if (getActivity() != null) {
                    isUpdating = true;
                    getActivity().invalidateOptionsMenu();
                }
                for (String pair : chartsNamesSorted) {
                    chartsUpdater.queueChart(
                            (StockChartView) chartsMap.get(pair).findViewById(R.id.stockChartView),
                            pair);
                }
            }
        }
    }

    private interface Listener<T> {
        void onChartDownloaded(@NonNull T token, @NonNull String pair,
                               @NonNull final String[] data);
    }

    private class ChartsUpdater extends HandlerThread {

        private static final int MESSAGE_DOWNLOAD = 0;
        private static final String TAG = "ChartsUpdaterThread";
        private Handler workerHandler;
        private Handler responseHandler;
        private Map<StockChartView, String> requestMap
                = Collections.synchronizedMap(new HashMap<StockChartView, String>());
        private Listener<StockChartView> mListener;


        ChartsUpdater(Handler responseHandler) {
            super(TAG);
            this.responseHandler = responseHandler;
        }

        void createHandler() {
            workerHandler = new Handler(getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (msg.what == MESSAGE_DOWNLOAD) {
                        @SuppressWarnings("unchecked")
                        StockChartView token = (StockChartView) msg.obj;
                        handleRequest(token);
                    }
                    return true;
                }
            });
        }

        private void handleRequest(final StockChartView token) {
            final String pair = requestMap.get(token);
            if (pair == null) {
                return;
            }
            ChartDataDownloader chartDataDownloader = new ChartDataDownloader();
            final String[] data = chartDataDownloader.getChartData(pair);
            requestMap.remove(token);
            responseHandler.post(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (requestMap.isEmpty() && activity != null) {
                        isUpdating = false;
                        activity.invalidateOptionsMenu();
                    }
                    mListener.onChartDownloaded(token, pair, data);
                }
            });
        }

        void setListener(Listener<StockChartView> listener) {
            mListener = listener;
        }

        void queueChart(StockChartView token, String pair) {
            if (workerHandler == null) {
                throw new NullPointerException("Handler was not created. You can" +
                        " do it by calling createHandler()");
            }
            requestMap.put(token, pair);
            workerHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
        }

        void clearQueue() {
            workerHandler.removeMessages(MESSAGE_DOWNLOAD);
            requestMap.clear();
        }
    }

    private class ChartDataDownloader {

        String[] getChartData(String pair) {

            StringBuilder out = new StringBuilder();
            BufferedReader rd = null;
            pair = PairUtils.localToServer(pair);
            try {
                URL url = new URL(BtcEApplication.get(getActivity()).getHostUrl()
                        + (isToken(pair) ? "/tokens/" : "/exchange/") + pair);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
                connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));
                try {
                    connection.addRequestProperty("Cookie", "old_charts=1");
                    if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                        showError();
                        return null;
                    }
                    rd = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        out.append(line);
                    }
                    Pattern pattern = Pattern.compile("arrayToDataTable\\(\\[\\[(.+?)\\]\\], true");
                    Matcher matcher = pattern.matcher(out.toString());
                    if (matcher.find()) {
                        //chart data
                        return matcher.group(1).split("\\],\\[");
                    } else {
                        return null;
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (rd != null) {
                        rd.close();
                    }
                }
            } catch (IOException e) {
                Log.e(ChartDataDownloader.class.getSimpleName(), "Failed to get chart data", e);
            }
            return null;
        }

        private boolean isToken(String pair) {
            return pair.split("_")[0].length() == 5;
        }
    }

}


