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

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.DBWorker;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.Watcher;

public class NotifiersFragment extends Fragment {

    private Cursor mCursor;
    private CursorAdapter mCursorAdapter;
    private DBWorker mDbWorker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_notifiers, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView listView = (ListView) view.findViewById(R.id.watchers_list);

        mDbWorker = DBWorker.getInstance(getActivity());
        mCursor = mDbWorker.getNotifiers();
        mCursor.moveToFirst();

        mCursorAdapter = new CursorAdapter(getActivity(), mCursor, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context).inflate(R.layout.notifiers_item, parent, false);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                TextView typeView = (TextView) view.findViewById(R.id.NotifiersType);
                TextView valueView = (TextView) view.findViewById(R.id.NotifiersValue);
                TextView pairView = (TextView) view.findViewById(R.id.NotifiersPair);
                final int id = cursor.getInt(cursor.getColumnIndex("_id"));

                ImageView remove = (ImageView) view.findViewById(R.id.removeNotifier);
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDbWorker.removeNotifier(id);
                        mCursor = mDbWorker.getNotifiers();
                        mCursorAdapter.swapCursor(mCursor);
                    }
                });

                @Watcher int watcherType = cursor.getInt(
                        cursor.getColumnIndex(DBWorker.NOTIFIERS_TYPE_COLUMN));
                String pair = cursor.getString(
                        cursor.getColumnIndex(DBWorker.NOTIFIERS_PAIR_COLUMN));
                pairView.setText(pair);
                final String value = cursor.getString(
                        cursor.getColumnIndex(DBWorker.NOTIFIERS_VALUE_COLUMN));
                switch (watcherType) {
                    case Watcher.PANIC_BUY:
                        typeView.setText(R.string.watcher_panic_buy);
                        valueView.setText(value + "%");
                        break;
                    case Watcher.PANIC_SELL:
                        typeView.setText(R.string.watcher_panic_sell);
                        valueView.setText(value + "%");
                        break;
                    case Watcher.STOP_LOSS:
                        typeView.setText(R.string.watcher_stop_loss);
                        valueView.setText(value + " " + pair.substring(4));
                        break;
                    case Watcher.TAKE_PROFIT:
                        typeView.setText(R.string.watcher_take_profit);
                        valueView.setText(value + " " + pair.substring(4));
                        break;
                    default:
                        break;
                }
            }
        };

        listView.setAdapter(mCursorAdapter);
        listView.setEmptyView(view.findViewById(R.id.NoItems));

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.actions_add, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                showAddWatcherDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddWatcherDialog() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View dialogContentView = inflater
                .inflate(R.layout.notifiers_add_dialog, null);

        final TextView valueTitle = (TextView) dialogContentView.findViewById(R.id.ValueTitle);
        final TextView notifDesc = (TextView) dialogContentView.findViewById(R.id.NotifDescription);

        Spinner watcherTypeSpinner = (Spinner) dialogContentView.findViewById(R.id.TypeSpinner);

        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item,
                        new CharSequence[]{
                                getString(R.string.watcher_panic_sell),
                                getString(R.string.watcher_panic_buy),
                                getString(R.string.watcher_stop_loss),
                                getString(R.string.watcher_take_profit)

                        });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        watcherTypeSpinner.setAdapter(adapter);

        watcherTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        valueTitle.setText(R.string.watcher_value_delta);
                        notifDesc.setText(R.string.PanicSellDescription);
                        break;
                    case 1:
                        valueTitle.setText(R.string.watcher_value_delta);
                        notifDesc.setText(R.string.PanicBuyDescription);
                        break;
                    case 2:
                        valueTitle.setText(R.string.watcher_value_number);
                        notifDesc.setText(R.string.StopLossDescription);
                        break;
                    case 3:
                        valueTitle.setText(R.string.watcher_value_number);
                        notifDesc.setText(R.string.TakeProfitDescription);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.AddWatcherTitle)
                .setView(dialogContentView)
                .setNeutralButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.DialogSaveButton,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveWatcher((AlertDialog) dialog);
                            }
                        }
                ).show();
    }

    private void saveWatcher(AlertDialog watcherDialog) {
        String typeName = ((Spinner) watcherDialog.findViewById(R.id.TypeSpinner))
                .getSelectedItem().toString();
        @Watcher int type = 0;
        if (getString(R.string.watcher_panic_sell).equals(typeName)) {
            type = Watcher.PANIC_SELL;
        } else if (getString(R.string.watcher_panic_buy).equals(typeName)) {
            type = Watcher.PANIC_BUY;
        } else if (getString(R.string.watcher_stop_loss).equals(typeName)) {
            type = Watcher.STOP_LOSS;
        } else if (getString(R.string.watcher_take_profit).equals(typeName)) {
            type = Watcher.TAKE_PROFIT;
        } else {
            throw new RuntimeException("Unknown watcher type = " + typeName);
        }

        String pair = ((Spinner) watcherDialog.findViewById(R.id.PairSpinner))
                .getSelectedItem().toString();
        float value = Float.parseFloat(((EditText) watcherDialog.findViewById(R.id.NotifValue))
                .getText().toString());

        saveWatcher(type, pair, value);
    }

    private void saveWatcher(@Watcher int watcherType, @NonNull String pair, float value) {
        mDbWorker.addNewNotifier(watcherType, pair, value);
        mCursor = mDbWorker.getNotifiers();
        mCursorAdapter.swapCursor(mCursor);
    }

    @Override
    public void onDestroyView() {
        mCursor.close();
        super.onDestroyView();
    }
}
