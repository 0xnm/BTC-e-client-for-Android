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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.R;

import static com.QuarkLabs.BTCeClient.AppPreferences.KEY_CHECK_ENABLED;
import static com.QuarkLabs.BTCeClient.AppPreferences.KEY_CHECK_PERIOD;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String mDefaultCheckPeriodSummaryText;
    private String[] mCheckPeriodEntries;
    private String[] mCheckPeriodValues;

    private AppPreferences appPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();

        mDefaultCheckPeriodSummaryText = getString(R.string.CheckPeriodSummary);
        mCheckPeriodEntries = getResources().getStringArray(R.array.Periods);
        mCheckPeriodValues = getResources().getStringArray(R.array.PeriodsInMsecs);
        Preference checkEnabled = findPreference(KEY_CHECK_ENABLED);
        if (checkEnabled.isEnabled()) {
            findPreference(KEY_CHECK_PERIOD).setSummary(
                    mDefaultCheckPeriodSummaryText.replace(getString(R.string.NATitle), findCheckPeriodText()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        appPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        appPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String currentPeriodText = findCheckPeriodText();
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

    private String findCheckPeriodText() {
        String currentPeriodMillis = appPreferences.getCheckPeriodMillis();
        int index = 0;
        for (int i = 0; i < mCheckPeriodValues.length; i++) {
            if (mCheckPeriodValues[i].equals(currentPeriodMillis)) {
                index = i;
                break;
            }
        }
        return mCheckPeriodEntries[index];
    }
}
