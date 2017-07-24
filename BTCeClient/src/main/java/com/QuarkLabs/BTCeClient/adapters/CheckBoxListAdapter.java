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

package com.QuarkLabs.BTCeClient.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CheckBoxListAdapter extends BaseAdapter {

    private final String[] items;
    private final Set<String> values;
    private String scope;
    private final Context context;

    public CheckBoxListAdapter(Context context, String[] items, SettingsScope settingsScope) {
        this.items = Arrays.copyOf(items, items.length);
        this.context = context;
        if (settingsScope == SettingsScope.CHARTS) {
            scope = "ChartsToDisplay";
            values = new HashSet<>(PairUtils.getChartsToDisplayThatSupported(context));
        } else if (settingsScope == SettingsScope.PAIRS) {
            scope = "PairsToDisplay";
            values = new HashSet<>(PairUtils.getTickersToDisplayThatSupported(context));
        } else {
            throw new RuntimeException("Unsupported scope");
        }
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.checkbox, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.checkBox = (CheckBox) convertView;
            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked()) {
                        values.add(checkBox.getText().toString());
                    } else {
                        values.remove(checkBox.getText().toString());
                    }
                }
            });
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        CheckBox checkBox = viewHolder.checkBox;
        String text = (String) getItem(position);
        if (values.contains(text)) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        checkBox.setText(text);
        return checkBox;
    }

    public void saveValuesToPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(scope, values);
        editor.apply();
    }

    public enum SettingsScope {
        PAIRS,
        CHARTS
    }

    private static class ViewHolder {
        CheckBox checkBox;
    }

}
