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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.QuarkLabs.BTCeClient.R;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_API_KEY = "API_Key";
    public static final String KEY_API_SECRET = "API_Secret";
    public static final String KEY_CHECK_ENABLED = "check_enabled";
    public static final String KEY_CHECK_PERIOD = "check_period";
    private String mDefaultCheckPeriodSummaryText;
    private String[] mCheckPeriodEntries;
    private String[] mCheckPeriodValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this);
        mDefaultCheckPeriodSummaryText = getResources().getString(R.string.CheckPeriodSummary);
        mCheckPeriodEntries = getResources().getStringArray(R.array.Periods);
        mCheckPeriodValues = getResources().getStringArray(R.array.PeriodsInMsecs);
        Preference checkEnabled = findPreference(KEY_CHECK_ENABLED);
        if (checkEnabled.isEnabled()) {
            findPreference(KEY_CHECK_PERIOD).setSummary(
                    mDefaultCheckPeriodSummaryText.replace("N/A",
                            findCheckPeriodText(PreferenceManager.getDefaultSharedPreferences(getActivity())))
            );
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String currentPeriodText = findCheckPeriodText(sharedPreferences);
        if (key.equals(KEY_CHECK_ENABLED)) {
            boolean checkEnabled = sharedPreferences.getBoolean(key, false);
            Preference checkPeriod = findPreference(KEY_CHECK_PERIOD);
            if (checkEnabled) {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText
                        .replace("N/A", currentPeriodText));
            } else {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText);
            }
        } else if (key.equals(KEY_CHECK_PERIOD)) {
            Preference checkPeriod = findPreference(KEY_CHECK_PERIOD);
            checkPeriod.setSummary(mDefaultCheckPeriodSummaryText
                    .replace("N/A", currentPeriodText));
        }
    }

    private String findCheckPeriodText(SharedPreferences sharedPreferences) {
        String currentPeriodInMsecs = sharedPreferences.getString(KEY_CHECK_PERIOD, "60000");
        int index = 0;
        for (int i = 0; i < mCheckPeriodValues.length; i++) {
            if (mCheckPeriodValues[i].equals(currentPeriodInMsecs)) {
                index = i;
                break;
            }
        }
        return mCheckPeriodEntries[index];
    }
}
