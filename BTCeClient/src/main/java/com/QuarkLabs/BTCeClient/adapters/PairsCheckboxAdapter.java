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

package com.QuarkLabs.BTCeClient.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter.SettingsScope.CHARTS;
import static com.QuarkLabs.BTCeClient.adapters.PairsCheckboxAdapter.SettingsScope.PAIRS;

public class PairsCheckboxAdapter extends BaseAdapter {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;

    private final List<ViewModel> items = new ArrayList<>();
    private final Set<String> selectedPairs;
    private boolean hasSeparator;
    private final AppPreferences appPreferences;
    private final SettingsScope scope;
    private final Context context;

    public PairsCheckboxAdapter(@NonNull Context context, @NonNull Set<String> allPairs,
                                @NonNull SettingsScope settingsScope) {
        this.context = context;
        if (settingsScope == CHARTS) {
            selectedPairs = new HashSet<>(PairUtils.getChartsToDisplayThatSupported(context));
        } else if (settingsScope == PAIRS) {
            selectedPairs = new HashSet<>(PairUtils.getTickersToDisplayThatSupported(context));
        } else {
            throw new RuntimeException("Unsupported scope");
        }
        scope = settingsScope;
        appPreferences = BtcEApplication.get(context).getAppPreferences();

        List<String> tickers = new ArrayList<>();
        List<String> tokens = new ArrayList<>();

        for (String pair : allPairs) {
            if (isToken(pair)) {
                tokens.add(pair);
            } else {
                tickers.add(pair);
            }
        }

        Collections.sort(tickers);
        Collections.sort(tokens);

        for (String ticker : tickers) {
            ViewModel viewModel = new ViewModel();
            viewModel.type = Type.ITEM;
            viewModel.pair = ticker;
            items.add(viewModel);
        }

        if (!tokens.isEmpty()) {

            hasSeparator = true;
            ViewModel separator = new ViewModel();
            separator.type = Type.SEPARATOR;
            items.add(separator);

            for (String token : tokens) {
                ViewModel viewModel = new ViewModel();
                viewModel.type = Type.ITEM;
                viewModel.pair = token;
                items.add(viewModel);
            }
        }
    }

    public static boolean isToken(@NonNull String pair) {
        // yep, so dumb
        return pair.split("/")[0].length() == 5;
    }

    @Override
    public int getViewTypeCount() {
        return hasSeparator ? 2 : 1;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type == Type.SEPARATOR ? TYPE_SEPARATOR : TYPE_ITEM;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_ITEM:
                convertView = createOrBindItemType(position, convertView, parent);
                break;
            case TYPE_SEPARATOR:
                if (convertView == null) {
                    convertView = LayoutInflater.from(context)
                            .inflate(R.layout.view_pair_separator, parent, false);
                }
                break;
        }
        return convertView;
    }

    private View createOrBindItemType(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.view_pair_checkbox, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.checkBox = (CheckBox) convertView;
            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked()) {
                        selectedPairs.add(checkBox.getText().toString());
                    } else {
                        selectedPairs.remove(checkBox.getText().toString());
                    }
                }
            });
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        CheckBox checkBox = viewHolder.checkBox;
        String text = ((ViewModel) getItem(position)).pair;
        if (selectedPairs.contains(text)) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }

        checkBox.setText(text);
        return convertView;
    }

    public void saveValuesToPreferences() {
        switch (scope) {
            case CHARTS:
                appPreferences.setChartsToDisplay(selectedPairs);
                break;
            case PAIRS:
                appPreferences.setPairsToDisplay(selectedPairs);
                break;
            default:
                break;
        }
    }

    public enum SettingsScope {
        PAIRS,
        CHARTS
    }

    private static class ViewHolder {
        CheckBox checkBox;
    }

    private enum Type {
        ITEM,
        SEPARATOR
    }

    private class ViewModel {
        Type type;
        @Nullable
        String pair;
    }

}
