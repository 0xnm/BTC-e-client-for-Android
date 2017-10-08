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

package com.QuarkLabs.BTCeClient.ui.settings;

import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.data.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.utils.PairUtils;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.ExchangeInfo;

public class SettingsFragment extends PreferenceFragment
        implements LoaderManager.LoaderCallbacks<CallResult<ExchangeInfo>> {

    private static final int EXCHANGE_INFO_LOADER_ID = 0;
    private static final String IS_EXCHANGE_SYNC_ONGOING_KEY = "IS_EXCHANGE_SYNC_ONGOING";

    private String mDefaultCheckPeriodSummaryText;
    private String[] mCheckPeriodEntries;
    private String[] mCheckPeriodValues;

    private AppPreferences appPreferences;
    private boolean isExchangeSyncOngoing;
    @Nullable
    private ProgressDialog exchangeSyncProgressDialog;

    private final AppPreferences.Listener appPrefListener = new AppPreferences.Listener() {
        @Override
        public void onCheckStatus(boolean isEnabled, @Nullable String periodMillis) {
            String currentPeriodText = findCheckPeriodText();
            final String keyCheckPeriod = getString(R.string.settings_key_check_period);
            Preference checkPeriod = findPreference(keyCheckPeriod);
            if (isEnabled) {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText
                        .replace(getString(R.string.NATitle), currentPeriodText));
            } else {
                checkPeriod.setSummary(mDefaultCheckPeriodSummaryText);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();

        mDefaultCheckPeriodSummaryText = getString(R.string.CheckPeriodSummary);
        mCheckPeriodEntries = getResources().getStringArray(R.array.Periods);
        mCheckPeriodValues = getResources().getStringArray(R.array.PeriodsInMsecs);
        Preference checkEnabled = findPreference(getString(R.string.settings_key_check_enabled));
        if (checkEnabled.isEnabled()) {
            findPreference(getString(R.string.settings_key_check_period)).setSummary(
                    mDefaultCheckPeriodSummaryText.replace(getString(R.string.NATitle),
                            findCheckPeriodText()));
        }
        findPreference(getString(R.string.settings_key_sync_exchange_pairs))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        isExchangeSyncOngoing = true;
                        showExchangeOngoingProgressDialog();
                        getLoaderManager().restartLoader(
                                EXCHANGE_INFO_LOADER_ID, null, SettingsFragment.this);
                        return true;
                    }
                });

        final EditTextPreference exchangeUrlPreference = (EditTextPreference) findPreference(
                getString(R.string.settings_key_exchange_url));
        exchangeUrlPreference.setSummary(appPreferences.getExchangeUrl());

        exchangeUrlPreference.setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String newUrl = (String) newValue;
                        if (!newUrl.startsWith("https://")) {
                            showNonHttpsWarning(exchangeUrlPreference, newUrl);
                            return false;
                        }
                        exchangeUrlPreference.setSummary(newUrl);
                        return true;
                    }
                });
    }

    private void showNonHttpsWarning(@NonNull final EditTextPreference preference,
                                     @NonNull final String newValue) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.exchange_url_no_https_warning)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                preference.setText(newValue);
                                preference.setSummary(newValue);
                            }
                        })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null
                && savedInstanceState.getBoolean(IS_EXCHANGE_SYNC_ONGOING_KEY, false)) {
            showExchangeOngoingProgressDialog();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        appPreferences.addListener(appPrefListener);
    }

    private void showExchangeOngoingProgressDialog() {
        exchangeSyncProgressDialog = new ProgressDialog(getActivity());
        exchangeSyncProgressDialog.setCancelable(false);
        exchangeSyncProgressDialog.setCanceledOnTouchOutside(false);
        exchangeSyncProgressDialog.setMessage(getString(R.string.msg_syncing_with_exchange));
        exchangeSyncProgressDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        appPreferences.removeListener(appPrefListener);
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_EXCHANGE_SYNC_ONGOING_KEY, isExchangeSyncOngoing);
    }

    @Override
    public Loader<CallResult<ExchangeInfo>> onCreateLoader(int id, Bundle args) {
        return new ExchangeInfoLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<CallResult<ExchangeInfo>> loader,
                               CallResult<ExchangeInfo> result) {
        isExchangeSyncOngoing = false;
        if (exchangeSyncProgressDialog != null) {
            exchangeSyncProgressDialog.dismiss();
            exchangeSyncProgressDialog = null;
        }
        if (result.isSuccess()) {
            //noinspection ConstantConditions
            appPreferences.setExchangePairs(PairUtils.exchangePairs(result.getPayload()));
        } else {
            Toast.makeText(getActivity(), R.string.exchange_sync_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLoaderReset(Loader<CallResult<ExchangeInfo>> loader) {
        // do nothing
    }
}
