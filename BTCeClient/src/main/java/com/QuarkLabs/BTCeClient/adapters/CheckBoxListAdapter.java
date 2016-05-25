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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import com.QuarkLabs.BTCeClient.PairUtils;
import com.QuarkLabs.BTCeClient.R;

import java.util.HashSet;
import java.util.Set;

public class CheckBoxListAdapter extends BaseAdapter {
    private final String[] mItems;
    private final Set<String> mSet;
    private String mScope;
    private Context mContext;

    public CheckBoxListAdapter(Context context, String[] items, SettingsScope settingsScope) {
        mItems = items;
        mContext = context;
        if (settingsScope == SettingsScope.CHARTS) {
            mScope = "ChartsToDisplay";
            mSet = new HashSet<>(PairUtils.getChartsToDisplayThatSupported(context));
        } else if (settingsScope == SettingsScope.PAIRS) {
            mScope = "PairsToDisplay";
            mSet = new HashSet<>(PairUtils.getTickersToDisplayThatSupported(context));
        } else {
            throw new RuntimeException("Unsupported scope");
        }
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public Object getItem(int position) {
        return mItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.checkbox, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.checkBox = (CheckBox) convertView;
            viewHolder.checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (checkBox.isChecked()) {
                        mSet.add(checkBox.getText().toString());
                    } else {
                        mSet.remove(checkBox.getText().toString());
                    }
                }
            });
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        CheckBox checkBox = viewHolder.checkBox;
        String text = (String) getItem(position);
        if (mSet.contains(text)) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }
        checkBox.setText(text);
        return checkBox;
    }

    public void saveValuesToPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(mScope, mSet);
        editor.commit();
    }

    public enum SettingsScope {
        PAIRS,
        CHARTS
    }

    private static class ViewHolder {
        CheckBox checkBox;
    }

}
