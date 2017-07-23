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
    public static final String KEY_USE_MIRROR = "use_mirror";
    public static final String KEY_USE_OLD_CHARTS = "use_btce_charts";

    private String mDefaultCheckPeriodSummaryText;
    private String[] mCheckPeriodEntries;
    private String[] mCheckPeriodValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.getDefaultSharedPreferences(
                getActivity()).registerOnSharedPreferenceChangeListener(this);
        mDefaultCheckPeriodSummaryText = getString(R.string.CheckPeriodSummary);
        mCheckPeriodEntries = getResources().getStringArray(R.array.Periods);
        mCheckPeriodValues = getResources().getStringArray(R.array.PeriodsInMsecs);
        Preference checkEnabled = findPreference(KEY_CHECK_ENABLED);
        if (checkEnabled.isEnabled()) {
            findPreference(KEY_CHECK_PERIOD).setSummary(
                    mDefaultCheckPeriodSummaryText.replace(getString(R.string.NATitle),
                            findCheckPeriodText(PreferenceManager
                                    .getDefaultSharedPreferences(getActivity())))
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(
                getActivity()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(
                getActivity()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String currentPeriodText = findCheckPeriodText(sharedPreferences);
        if (KEY_CHECK_ENABLED.equals(key)) {
            boolean checkEnabled = sharedPreferences.getBoolean(key, false);
            Preference checkPeriod = findPreference(KEY_CHECK_PERIOD);
            if (checkEnabled) {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText
                        .replace(getString(R.string.NATitle), currentPeriodText));
            } else {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText);
            }
        } else if (KEY_CHECK_PERIOD.equals(key)) {
            Preference checkPeriod = findPreference(KEY_CHECK_PERIOD);
            checkPeriod.setSummary(mDefaultCheckPeriodSummaryText
                    .replace(getString(R.string.NATitle), currentPeriodText));
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
