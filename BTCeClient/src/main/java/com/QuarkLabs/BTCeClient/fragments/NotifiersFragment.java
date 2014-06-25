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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.QuarkLabs.BTCeClient.DBWorker;
import com.QuarkLabs.BTCeClient.R;

public class NotifiersFragment extends Fragment {
    private final static int PANIC_BUY_TYPE = 0;
    private final static int PANIC_SELL_TYPE = 1;
    private final static int STOP_LOSS_TYPE = 2;
    private final static int TAKE_PROFIT_TYPE = 3;
    private Cursor mCursor;
    private CursorAdapter mCursorAdapter;
    private DBWorker mDbWorker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_notifiers, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView listView = (ListView) ((ViewGroup) getView()).getChildAt(0);
        mDbWorker = DBWorker.getInstance(getActivity());
        mCursor = mDbWorker.getNotifiers();
        mCursor.moveToFirst();
        mCursorAdapter = new CursorAdapter(getActivity(), mCursor, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                return inflater.inflate(R.layout.notifiers_item, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView type = (TextView) view.findViewById(R.id.NotifiersType);
                TextView value = (TextView) view.findViewById(R.id.NotifiersValue);
                TextView pair = (TextView) view.findViewById(R.id.NotifiersPair);
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
                int typeValue = cursor.getInt(cursor.getColumnIndex("Type"));
                String pairText = cursor.getString(cursor.getColumnIndex("Pair"));
                pair.setText(pairText);
                switch (typeValue) {
                    case PANIC_BUY_TYPE:
                        type.setText("Panic Buy");
                        value.setText(cursor.getString(cursor.getColumnIndex("Value")) + "%");
                        break;
                    case PANIC_SELL_TYPE:
                        type.setText("Panic Sell");
                        value.setText(cursor.getString(cursor.getColumnIndex("Value")) + "%");
                        break;
                    case STOP_LOSS_TYPE:
                        type.setText("Stop Loss");
                        value.setText(cursor.getString(cursor.getColumnIndex("Value")) + " " + pairText.substring(4));
                        break;
                    case TAKE_PROFIT_TYPE:
                        type.setText("Take Profit");
                        value.setText(cursor.getString(cursor.getColumnIndex("Value")) + " " + pairText.substring(4));
                        break;
                    default:
                        break;
                }
            }
        };

        listView.setAdapter(mCursorAdapter);
        listView.setEmptyView(getView().findViewById(R.id.NoItems));

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
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.notifiers_add_dialog, null);
                final TextView valueTitle = (TextView) view.findViewById(R.id.ValueTitle);
                final TextView notifDesc = (TextView) view.findViewById(R.id.NotifDescription);
                Spinner type = (Spinner) view.findViewById(R.id.TypeSpinner);
                type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:
                                valueTitle.setText("Delta (%)");
                                notifDesc.setText(getActivity().getResources()
                                        .getString(R.string.PanicSellDescription));
                                break;
                            case 1:
                                valueTitle.setText("Delta (%)");
                                notifDesc.setText(getActivity().getResources()
                                        .getString(R.string.PanicBuyDescription));
                                break;
                            case 2:
                                valueTitle.setText("Value");
                                notifDesc.setText(getActivity().getResources()
                                        .getString(R.string.StopLossDescription));
                                break;
                            case 3:
                                valueTitle.setText("Value");
                                notifDesc.setText(getActivity().getResources()
                                        .getString(R.string.TakeProfitDescription));
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
                        .setTitle(getActivity().getString(R.string.AddNotifierTitle))
                        .setView(view)
                        .setNeutralButton(getResources().getString(R.string.DialogSave),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        AlertDialog alertDialog = (AlertDialog) dialog;
                                        String typeString = ((Spinner) alertDialog.findViewById(R.id.TypeSpinner))
                                                .getSelectedItem().toString();
                                        int type = 0;
                                        if (typeString.equals("Panic Sell")) {
                                            type = 1;
                                        } else if (typeString.equals("Panic Buy")) {
                                            type = 0;
                                        } else if (typeString.equals("Stop-loss")) {
                                            type = 2;
                                        } else if (typeString.equals("Take-profit")) {
                                            type = 3;
                                        }

                                        String pair = ((Spinner) alertDialog.findViewById(R.id.PairSpinner))
                                                .getSelectedItem().toString();
                                        float value = Float.parseFloat(((EditText) alertDialog.findViewById(R.id.NotifValue))
                                                .getText().toString());


                                        mDbWorker.addNewNotifier(type, pair, value);
                                        mCursor = mDbWorker.getNotifiers();
                                        mCursorAdapter.swapCursor(mCursor);
                                    }
                                }
                        ).show();

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCursor.close();
    }
}
