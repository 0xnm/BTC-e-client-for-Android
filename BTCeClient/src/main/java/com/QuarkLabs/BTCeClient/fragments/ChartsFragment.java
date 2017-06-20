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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.CheckBoxListAdapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ChartsFragment extends Fragment {

    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_chart, container, false);
        }
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        refreshChartViews();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshChartViews();
                break;
            case R.id.action_add:
                final CheckBoxListAdapter checkBoxListAdapter =
                        new CheckBoxListAdapter(getActivity(),
                                getResources().getStringArray(R.array.ExchangePairs),
                                CheckBoxListAdapter.SettingsScope.CHARTS);
                ListView v = new ListView(getActivity());
                v.setAdapter(checkBoxListAdapter);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.SelectPairsPromptTitle)
                        .setView(v)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkBoxListAdapter.saveValuesToPreferences();
                                refreshChartViews();
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
     * Creates views for charts
     */
    private void refreshChartViews() {

        LinearLayout chartsContainer = (LinearLayout) mRootView.findViewById(R.id.ChartsContainer);
        chartsContainer.removeAllViews();

        Set<String> hashSet = new HashSet<>(
                PairUtils.getChartsToDisplayThatSupported(getActivity()));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
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

}


