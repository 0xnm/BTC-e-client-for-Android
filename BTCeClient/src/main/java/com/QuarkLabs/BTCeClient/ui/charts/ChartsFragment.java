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

package com.QuarkLabs.BTCeClient.ui.charts;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
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

import com.QuarkLabs.BTCeClient.data.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.MainNavigator;
import com.QuarkLabs.BTCeClient.utils.PageDownloader;
import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter;
import com.QuarkLabs.BTCeClient.api.Api;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.Ticker;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
import org.stockchart.series.BarSeries;
import org.stockchart.series.SeriesBase;
import org.stockchart.series.StockSeries;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartsFragment extends Fragment {

    private ChartsDelegate chartsDelegate;
    private boolean isUpdating;
    private View rootView;
    private AlertDialog chartsDialog;
    private AppPreferences appPreferences;

    private MainNavigator mainNavigator;

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
        mainNavigator = (MainNavigator) getActivity();
        setHasOptionsMenu(true);
        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();
        boolean useOldCharts = BtcEApplication.get(getActivity())
                .getAppPreferences().isShowOldCharts();
        chartsDelegate = new BtceChartsDelegate();
        chartsDelegate.onViewCreated();
        chartsDelegate.createChartViews(view);
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
                ListView pairsView = new ListView(getActivity());
                pairsView.setAdapter(pairsCheckboxAdapter);
                chartsDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.SelectPairsPromptTitle)
                        .setView(pairsView)
                        .setNeutralButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pairsCheckboxAdapter.saveValuesToPreferences();
                                        chartsDelegate.createChartViews(getView());
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
        mainNavigator = null;
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

        void createChartViews(@NonNull View rootView);

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
        public void createChartViews(@NonNull View rootView) {
            LinearLayout chartsContainer = (LinearLayout) rootView
                    .findViewById(R.id.ChartsContainer);
            chartsContainer.removeAllViews();

            List<String> pairs = appPreferences.getChartsToDisplay();

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
            if (pairs.isEmpty()) {
                chartsContainer.addView(noCharts);
            }

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
                    "<script type='text/javascript'" +
                    " src='https://d33t3vvu2t2yu5.cloudfront.net/tv.js'></script>\n" +
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

        //CHECKSTYLE:OFF
        private final DecimalFormat PRICE_DECIMAL_FORMAT = new DecimalFormat("#.#####");

        private final int VOLUME_BAR_COLOR = Color.parseColor("#ebebeb");
        private final int RISING_FILL_COLOR = Color.parseColor("#0ab92b");
        private final int FALLING_FILL_COLOR = Color.parseColor("#f01717");
        private final int STICK_COLOR = Color.parseColor("#515151");
        //CHECKSTYLE:ON

        private static final String PRICE_SERIES_NAME = "price";
        private static final String VOLUME_SERIES_NAME = "volume";

        private Map<String, View> chartsMap;
        private ChartsUpdater chartsUpdater;
        private Handler responseHandler;

        private String priceText;
        private String volumeText;

        @Override
        public void onViewCreated() {
            responseHandler = new Handler();
            chartsUpdater = new ChartsUpdater(responseHandler);
            chartsUpdater.setListener(new Listener<View>() {
                @Override
                public void onChartDownloaded(@NonNull View chartView,
                                              @NonNull final String pair,
                                              @NonNull final ChartData data) {
                    updateChart(chartView, pair, data);
                }
            });

            chartsUpdater.start();
            chartsUpdater.createHandler();

            priceText = getString(R.string.charts_price_label);
            volumeText = getString(R.string.charts_volume_label);
        }

        @Override
        public void onDestroyView() {
            chartsUpdater.clearQueue();
            chartsUpdater.quit();
        }

        private void updateChart(@NonNull View chartView, @NonNull final String pair,
                                 @NonNull final ChartData data) {
            StockChartView stockChartView =
                    (StockChartView) chartView.findViewById(R.id.stockChartView);


            View tradeButton = chartView.findViewById(R.id.chart_trade_button);
            TextView namePriceView = (TextView) chartView.findViewById(R.id.chart_name_price);
            if (data.last != null) {
                tradeButton.setEnabled(true);
                tradeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mainNavigator.openTradingSection(pair, new BigDecimal(data.last));
                    }
                });
                namePriceView.setText(String.format("%s: %s", pair, data.last));
            } else {
                tradeButton.setEnabled(false);
                tradeButton.setOnClickListener(null);
                namePriceView.setText(String.format("%s: %s", pair, "–"));
            }


            stockChartView.reset();
            stockChartView.getCrosshair().setAuto(true);

            final StockSeries priceSeries = new StockSeries();
            priceSeries.setViewType(StockSeries.ViewType.CANDLESTICK);
            priceSeries.setName(PRICE_SERIES_NAME);
            // by some reason in the lib these appearances are messed, rise <-> fall
            Appearance riseAppearance = priceSeries.getFallAppearance();
            riseAppearance.setFillColors(RISING_FILL_COLOR);
            riseAppearance.setOutlineColor(STICK_COLOR);

            Appearance fallAppearance = priceSeries.getRiseAppearance();
            fallAppearance.setFillColors(FALLING_FILL_COLOR);
            fallAppearance.setOutlineColor(STICK_COLOR);

            final BarSeries volumeSeries = new BarSeries();
            volumeSeries.setName(VOLUME_SERIES_NAME);
            volumeSeries.setYAxisSide(Axis.Side.LEFT);
            volumeSeries.getAppearance().setAllColors(VOLUME_BAR_COLOR);

            Area area = stockChartView.addArea();
            area.getAppearance().setOutlineColor(Color.TRANSPARENT);
            TypedValue a = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                // windowBackground is a color
                int color = a.data;
                area.getAppearance().setPrimaryFillColor(color);
            }

            for (HistoryItem historyItem : data.history) {
                priceSeries.addPoint(historyItem.open,
                        historyItem.high,
                        historyItem.low,
                        historyItem.close);

                volumeSeries.addPoint(0, historyItem.volume);
            }

            area.getSeries().add(volumeSeries);
            area.getSeries().add(priceSeries);

            //provider for bottom axis (time)
            Axis.ILabelFormatProvider bottomLfp = new Axis.ILabelFormatProvider() {
                @Override
                public String getAxisLabel(Axis axis, double v) {
                    int index = priceSeries.convertToArrayIndex(v);
                    if (index < 0) {
                        index = 0;
                    }
                    if (index >= 0) {
                        if (index >= priceSeries.getPointCount()) {
                            index = priceSeries.getPointCount() - 1;
                        }

                        return data.history.get(index).time.replace("\"", "");
                    }
                    return null;
                }
            };
            //provider for crosshair
            Crosshair.ILabelFormatProvider dp = new Crosshair.ILabelFormatProvider() {
                @Override
                public String getLabel(Crosshair crosshair, Plot plot, double v, double v2) {
                    int index = priceSeries.convertToArrayIndex(v);
                    if (index < 0) {
                        index = 0;
                    }
                    if (index >= 0) {
                        if (index >= priceSeries.getPointCount()) {
                            index = priceSeries.getPointCount() - 1;
                        }

                        return data.history.get(index).time.replace("\"", "")
                                + String.format(": %s ", priceText)
                                + PRICE_DECIMAL_FORMAT.format(v2)
                                + String.format(", %s ", volumeText)
                                + String.valueOf((int) (data.history.get(index).volume + 0.5f));
                    }
                    return null;
                }
            };

            stockChartView.getCrosshair().setLabelFormatProvider(dp);

            area.getRightAxis().setLabelFormatProvider(new Axis.ILabelFormatProvider() {
                @Override
                public String getAxisLabel(Axis axis, double v) {
                    return PRICE_DECIMAL_FORMAT.format(v);
                }
            });
            area.getBottomAxis().setLabelFormatProvider(bottomLfp);
            area.getLeftAxis().setVisible(false);

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            Resources resources = getResources();
            float axisFontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13,
                    resources.getDisplayMetrics());

            //styling: setting fonts

            area.getBottomAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                            resources.getDisplayMetrics()));
            area.getRightAxis()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42,
                            resources.getDisplayMetrics()));
            area.getBottomAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                            resources.getDisplayMetrics()));
            area.getRightAxis()
                    .getAppearance()
                    .getFont()
                    .setSize(axisFontSize);
            stockChartView.getCrosshair().getAppearance().getFont()
                    .setSize(axisFontSize);
            stockChartView.invalidate();
        }

        @Override
        public void createChartViews(@NonNull View rootView) {
            LinearLayout chartsContainer = (LinearLayout)
                    rootView.findViewById(R.id.ChartsContainer);
            chartsContainer.removeAllViews();

            chartsMap = new HashMap<>();
            List<String> pairs = appPreferences.getChartsToDisplay();

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
            if (pairs.isEmpty()) {
                chartsContainer.addView(noCharts);
            }

            for (String pair : pairs) {
                View chartView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.chart_item, chartsContainer, false);
                TextView namePriceView = (TextView) chartView.findViewById(R.id.chart_name_price);
                namePriceView.setText(String.format("%s: –", pair));
                View tradeButton = chartView.findViewById(R.id.chart_trade_button);
                tradeButton.setEnabled(false);
                tradeButton.setOnClickListener(null);
                chartsContainer.addView(chartView);
                chartsMap.put(pair, chartView);
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
                    SeriesBase priceSeries = stockChartView.findSeriesByName(PRICE_SERIES_NAME);
                    if (priceSeries != null) {
                        if (isChecked) {
                            onIndicatorEnabled(stockChartView, buttonView, priceSeries);
                        } else {
                            onIndicatorDisabled(stockChartView, buttonView);
                        }
                        stockChartView.recalcIndicators();
                        stockChartView.recalc();
                        stockChartView.invalidate();
                    } else {
                        //if chart has no data, ignore switch
                        SwitchCompat sw = (SwitchCompat) buttonView;
                        sw.setChecked(!isChecked);
                    }
                }
            };

            //add listeners to switches
            for (String pair : chartsMap.keySet()) {
                View chartView = chartsMap.get(pair);
                ((SwitchCompat) chartView.findViewById(R.id.enableEMAIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartView.findViewById(R.id.enableRSIIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartView.findViewById(R.id.enableSMAIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
                ((SwitchCompat) chartView.findViewById(R.id.enableMACDIndicator))
                        .setOnCheckedChangeListener(indicatorChangeStateListener);
            }
        }

        private void onIndicatorEnabled(@NonNull StockChartView chartView,
                                        @NonNull View switchView,
                                        @NonNull SeriesBase priceSeries) {
            IndicatorManager indicatorManager = chartView.getIndicatorManager();
            //if switch turned on and chart has data
            switch (switchView.getId()) {
                case R.id.enableEMAIndicator:
                    indicatorManager.addEma(priceSeries, 0);
                    break;
                case R.id.enableMACDIndicator:
                    indicatorManager.addMacd(priceSeries, 0);
                    break;
                case R.id.enableRSIIndicator:
                    indicatorManager.addRsi(priceSeries, 0);
                    break;
                case R.id.enableSMAIndicator:
                    indicatorManager.addSma(priceSeries, 0);
                    break;
                default:
                    break;
            }
        }

        private void onIndicatorDisabled(@NonNull StockChartView chartView,
                                         @NonNull View switchView) {
            //if switch turned off and chart has no data
            Iterator<AbstractIndicator> iterator = chartView
                    .getIndicatorManager()
                    .getIndicators()
                    .iterator();
            while (iterator.hasNext()) {
                AbstractIndicator x = iterator.next();
                boolean shouldRemoveCurrent = false;
                switch (switchView.getId()) {
                    case R.id.enableEMAIndicator:
                        shouldRemoveCurrent = x instanceof EmaIndicator;
                        break;
                    case R.id.enableMACDIndicator:
                        shouldRemoveCurrent = x instanceof MacdIndicator;
                        break;
                    case R.id.enableRSIIndicator:
                        shouldRemoveCurrent = x instanceof RsiIndicator;
                        break;
                    case R.id.enableSMAIndicator:
                        shouldRemoveCurrent = x instanceof SmaIndicator;
                        break;
                    default:
                        break;
                }
                if (shouldRemoveCurrent) {
                    iterator.remove();
                    chartView.getIndicatorManager().removeIndicator(x);
                }
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
            if (!chartsMap.isEmpty()) {
                List<String> chartNames = new ArrayList<>(chartsMap.keySet());
                Collections.sort(chartNames, PairUtils.CURRENCY_COMPARATOR);
                if (getActivity() != null) {
                    isUpdating = true;
                    getActivity().invalidateOptionsMenu();
                }
                for (String pair : chartNames) {
                    chartsUpdater.queueChart(chartsMap.get(pair), pair);
                }
            }
        }
    }

    private interface Listener<T> {
        void onChartDownloaded(@NonNull View token,
                               @NonNull String pair,
                               @NonNull ChartData data);
    }

    private class ChartsUpdater extends HandlerThread {

        private static final int MESSAGE_DOWNLOAD = 0;
        private static final String TAG = "ChartsUpdaterThread";
        private Handler workerHandler;
        private final Handler responseHandler;
        private final Map<View, String> requestMap
                = Collections.synchronizedMap(new HashMap<View, String>());
        private Listener<View> mListener;


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
                        View token = (View) msg.obj;
                        handleRequest(token);
                    }
                    return true;
                }
            });
        }

        private void handleRequest(final View token) {
            final String pair = requestMap.get(token);
            if (pair == null) {
                return;
            }
            ChartDataDownloader chartDataDownloader = new ChartDataDownloader();
            final ChartData data = chartDataDownloader.getChartData(pair);
            requestMap.remove(token);
            responseHandler.post(new Runnable() {
                @Override
                public void run() {
                    Activity activity = getActivity();
                    if (activity != null) {
                        if (requestMap.isEmpty()) {
                            isUpdating = false;
                            activity.invalidateOptionsMenu();
                        }
                        if (data != null) {
                            mListener.onChartDownloaded(token, pair, data);
                        }
                    }
                }
            });
        }

        void setListener(@NonNull Listener<View> listener) {
            mListener = listener;
        }

        void queueChart(@NonNull View chartView, @NonNull String pair) {
            if (workerHandler == null) {
                throw new NullPointerException("Handler was not created. You can" +
                        " do it by calling createHandler()");
            }
            requestMap.put(chartView, pair);
            workerHandler.obtainMessage(MESSAGE_DOWNLOAD, chartView).sendToTarget();
        }

        void clearQueue() {
            workerHandler.removeMessages(MESSAGE_DOWNLOAD);
            requestMap.clear();
        }
    }

    private class ChartDataDownloader {

        @Nullable
        @WorkerThread
        ChartData getChartData(@NonNull String pair) {

            String content = new PageDownloader().download(appPreferences.getExchangeUrl(),
                    pair, null);

            if (content == null) {
                showError();
                return null;
            }

            Pattern pattern = Pattern.compile("arrayToDataTable\\(\\[\\[(.+?)\\]\\], true");
            Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                return null;
            }
            ChartData chartData = new ChartData();
            chartData.history = new ArrayList<>();
            for (String item : matcher.group(1).split("\\],\\[")) {
                String[] itemParts = item.split(",");
                HistoryItem historyItem = new HistoryItem();
                historyItem.time = itemParts[0];
                historyItem.low = Double.parseDouble(itemParts[1]);
                historyItem.open = Double.parseDouble(itemParts[2]);
                historyItem.close = Double.parseDouble(itemParts[3]);
                historyItem.high = Double.parseDouble(itemParts[4]);
                historyItem.volume = Double.parseDouble(itemParts[5]);
                chartData.history.add(historyItem);
            }

            Document document = Jsoup.parse(content);
            Elements pairsHostElements = document.getElementsByClass("pairs-selected");
            if (pairsHostElements == null || pairsHostElements.size() != 1) {
                chartData.last = tryGetPriceFromApi(pair);
            } else {
                Elements pairValueElements =
                        pairsHostElements.get(0).getElementsByAttributeValueStarting("id", "last");
                if (pairValueElements == null || pairValueElements.size() != 1) {
                    chartData.last = tryGetPriceFromApi(pair);
                } else {
                    chartData.last = pairValueElements.get(0).html();
                }
            }
            return chartData;
        }

        @Nullable
        @WorkerThread
        private String tryGetPriceFromApi(@NonNull String pair) {
            Context context = getActivity();
            if (context != null) {
                Api api = BtcEApplication.get(context).getApi();
                CallResult<List<Ticker>> callResult = api.getPairInfo(Collections.singleton(pair));
                if (callResult.isSuccess()) {
                    return callResult.getPayload().get(0).getLast().toPlainString();
                }
            }
            return null;
        }
    }

    private class ChartData {
        List<HistoryItem> history;
        String last;
    }

    private class HistoryItem {
        String time;
        double open;
        double high;
        double low;
        double close;
        double volume;
    }

}


