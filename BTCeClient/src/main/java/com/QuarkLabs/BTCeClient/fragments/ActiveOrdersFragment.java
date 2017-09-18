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

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.ActiveOrdersAdapter;
import com.QuarkLabs.BTCeClient.api.ActiveOrder;
import com.QuarkLabs.BTCeClient.api.CallResult;
import com.QuarkLabs.BTCeClient.api.CancelOrderResponse;
import com.QuarkLabs.BTCeClient.loaders.ActiveOrdersLoader;
import com.QuarkLabs.BTCeClient.tasks.ApiResultListener;
import com.QuarkLabs.BTCeClient.tasks.CancelActiveOrderTask;

import java.util.List;
import java.util.Locale;

public class ActiveOrdersFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<CallResult<List<ActiveOrder>>>,
        ApiResultListener<CancelOrderResponse>,
        ActiveOrdersAdapter.OnCancelOrderClickListener {

    private static final int LOADER_ID = 2;
    private ActiveOrdersAdapter ordersAdapter;
    private ListView ordersView;
    private ProgressBar loadingView;
    private TextView errorView;
    private AlertDialog cancelOrderDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        return inflater.inflate(R.layout.fragment_active_orders, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        ordersView = (ListView) getView().findViewById(R.id.ActiveOrdersContainer);
        ordersAdapter = new ActiveOrdersAdapter();
        ordersAdapter.setOnCancelOrderClickListener(this);
        loadingView = (ProgressBar) getView().findViewById(R.id.Loading);
        errorView = (TextView) getView().findViewById(R.id.NoItems);
        ordersView.setEmptyView(loadingView);
    }

    @Override
    public Loader<CallResult<List<ActiveOrder>>> onCreateLoader(int id, Bundle args) {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
        if (loadingView != null) {
            ordersView.setEmptyView(loadingView);
        }
        return new ActiveOrdersLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<CallResult<List<ActiveOrder>>> loader,
                               CallResult<List<ActiveOrder>> data) {
        if (!data.isSuccess()) {
            Toast.makeText(
                    getActivity(),
                    getString(R.string.general_error_text), Toast.LENGTH_LONG).show();
            errorView.setText(data.getError().toUpperCase(Locale.US));
            ordersView.setEmptyView(errorView);
            loadingView.setVisibility(View.GONE);
        } else {
            ordersAdapter.setActiveOrders(data.getPayload());
            ordersView.setAdapter(ordersAdapter);
            loadingView.setVisibility(View.GONE);
            errorView.setText(R.string.no_items_text);
            ordersView.setEmptyView(errorView);
        }
    }

    @Override
    public void onLoaderReset(Loader<CallResult<List<ActiveOrder>>> loader) {
        ordersView.setAdapter(null);
        errorView.setVisibility(View.GONE);
        ordersView.setEmptyView(loadingView);
    }

    @Override
    public void onCancelOrderClicked(final long orderId) {
        cancelOrderDialog = new AlertDialog.Builder(getActivity())
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new CancelActiveOrderTask(
                                BtcEApplication.get(getActivity()).getApi(),
                                ActiveOrdersFragment.this).execute(orderId);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setMessage(getString(R.string.order_delete_question, orderId))
                .show();
    }

    @Override
    public void onSuccess(@NonNull CancelOrderResponse result) {
        ordersAdapter.removeOrder(result.getOrderId());
        notifyAboutOrderDeletionResult(getString(R.string.order_deleted_successfully));
    }

    @Override
    public void onError(@NonNull String error) {
        notifyAboutOrderDeletionResult(error);
    }

    private void notifyAboutOrderDeletionResult(@NonNull String text) {
        NotificationCompat.Builder mBuilder = new NotificationCompat
                .Builder(getActivity())
                .setSmallIcon(R.drawable.ic_stat_bitcoin_sign)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(text)
                .setColor(getResources().getColor(R.color.colorPrimary));

        mBuilder.setSound(RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        NotificationManager mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(5, mBuilder.build());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cancelOrderDialog != null) {
            cancelOrderDialog.dismiss();
            cancelOrderDialog = null;
        }
    }
}
