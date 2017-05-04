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

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.QuarkLabs.BTCeClient.ListType;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.adapters.OrdersAdapter;
import com.QuarkLabs.BTCeClient.loaders.OrdersLoader;
import org.json.JSONObject;

import java.util.Locale;

public class ActiveOrdersFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<JSONObject> {

    private static final int LOADER_ID = 2;
    private OrdersAdapter mAdapter;
    private ListView mListView;
    private ProgressBar mLoadingView;
    private TextView mNoItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        return inflater.inflate(R.layout.fragment_active_orders, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        mListView = (ListView) getView().findViewById(R.id.ActiveOrdersContainer);
        mAdapter = new OrdersAdapter(getActivity(), ListType.ActiveOrders);
        mLoadingView = (ProgressBar) getView().findViewById(R.id.Loading);
        mNoItems = (TextView) getView().findViewById(R.id.NoItems);
        mListView.setEmptyView(mLoadingView);
    }

    @Override
    public Loader<JSONObject> onCreateLoader(int id, Bundle args) {
        if (mNoItems != null) {
            mNoItems.setVisibility(View.GONE);
        }
        if (mLoadingView != null) {
            mListView.setEmptyView(mLoadingView);
        }
        return new OrdersLoader(getActivity(), null, ListType.ActiveOrders);
    }

    @Override
    public void onLoadFinished(Loader<JSONObject> loader, JSONObject data) {
        if (data == null) {
            Toast.makeText(getActivity(), getString(R.string.GeneralErrorText), Toast.LENGTH_LONG)
                    .show();
            mNoItems.setText(getString(R.string.OoopsError).toUpperCase(Locale.US));
            mListView.setEmptyView(mNoItems);
            mLoadingView.setVisibility(View.GONE);
        } else if (data.optInt("success") == 0) {
            mNoItems.setText(data.optString("error").toUpperCase(Locale.US));
            mListView.setEmptyView(mNoItems);
            mLoadingView.setVisibility(View.GONE);
        } else {
            mAdapter.updateEntries(data);
            mListView.setAdapter(mAdapter);
            mLoadingView.setVisibility(View.GONE);
            mListView.setEmptyView(mNoItems);
        }
    }

    @Override
    public void onLoaderReset(Loader<JSONObject> loader) {
        mListView.setAdapter(null);
        mNoItems.setVisibility(View.GONE);
        mListView.setEmptyView(mLoadingView);
    }
}
