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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.QuarkLabs.BTCeClient.AuthRequest;
import com.QuarkLabs.BTCeClient.MyActivity;
import com.QuarkLabs.BTCeClient.R;

import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().getActionBar().setTitle(getResources().getStringArray(R.array.NavSections)[6]);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        final EditText SettingsKey = (EditText) getView().findViewById(R.id.SettingsKey);
        final EditText SettingsSecret = (EditText) getView().findViewById(R.id.SettingsSecret);
        final TextView SettingsCheckPeriod = (TextView) getView().findViewById(R.id.SettingsCheckingPeriod);
        Button SaveSettingsButton = (Button) getView().findViewById(R.id.SaveSettingsButton);

        final SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(getActivity());
        long milliseconds = sh.getLong("periodForChecking", 0);
        String text;
        if (milliseconds != 0 && MyActivity.alarmSet) {
            text = (milliseconds < 60 * 1000) ? TimeUnit.MILLISECONDS.toSeconds(milliseconds) + " sec."
                    : TimeUnit.MILLISECONDS.toMinutes(milliseconds) + " min.";
        } else {
            text = "N/A";
        }
        SettingsCheckPeriod.setText(getResources().getString(R.string.RecurrentPeriod) + " " + text);

        SettingsKey.setText(sh.getString("key", ""));
        SettingsSecret.setText(sh.getString("secret", ""));
        SaveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor ed = sh.edit();
                ed.putString("key", SettingsKey.getText().toString());
                AuthRequest.key = SettingsKey.getText().toString();
                ed.putString("secret", SettingsSecret.getText().toString());
                AuthRequest.secret = SettingsSecret.getText().toString();
                ed.commit();
            }
        });
        Switch alarmSelector = (Switch) getView().findViewById(R.id.AlarmSelector);
        alarmSelector.setChecked(MyActivity.alarmSet);
        alarmSelector.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final SharedPreferences.Editor editor = sh.edit();
                if (!isChecked) {

                    MyActivity.alarmManager.cancel(MyActivity.pendingIntent);
                    MyActivity.alarmSet = false;
                    editor.putBoolean("periodicalCheckEnabled", false);
                    SettingsCheckPeriod.setText(getResources().getString(R.string.RecurrentPeriod) + " N/A");
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getResources().getString(R.string.ChoosePeriod))
                            .setSingleChoiceItems(R.array.Periods, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AlertDialog dialog1 = (AlertDialog) dialog;

                                    String b = (String) dialog1.getListView().getItemAtPosition(which);
                                    long milliseconds = 1000;
                                    if (b.equals(getResources().getStringArray(R.array.Periods)[0])) {
                                        milliseconds = 30 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[1])) {
                                        milliseconds = 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[2])) {
                                        milliseconds = 2 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[3])) {
                                        milliseconds = 3 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[4])) {
                                        milliseconds = 5 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[5])) {
                                        milliseconds = 10 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[6])) {
                                        milliseconds = 15 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[7])) {
                                        milliseconds = 20 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[8])) {
                                        milliseconds = 30 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[9])) {
                                        milliseconds = 45 * 60 * 1000;
                                    } else if (b.equals(getResources().getStringArray(R.array.Periods)[10])) {
                                        milliseconds = 60 * 60 * 1000;
                                    }
                                    Toast.makeText(getActivity(),
                                            getResources().getString(R.string.PeriodWasSetFor) + " " + b,
                                            Toast.LENGTH_LONG)
                                            .show();
                                    editor.putLong("periodForChecking", milliseconds);
                                    editor.putBoolean("periodicalCheckEnabled", true);
                                    editor.commit();
                                    MyActivity.alarmSet = true;
                                    ((MyActivity) getActivity()).setRecurringAlarm(milliseconds);
                                    String text = (milliseconds < 60 * 1000) ?
                                            TimeUnit.MILLISECONDS.toSeconds(milliseconds) + " sec."
                                            : TimeUnit.MILLISECONDS.toMinutes(milliseconds) + " min.";
                                    SettingsCheckPeriod.setText(getResources()
                                            .getString(R.string.RecurrentPeriod) + " " + text);
                                    dialog.cancel();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        });
    }
}
