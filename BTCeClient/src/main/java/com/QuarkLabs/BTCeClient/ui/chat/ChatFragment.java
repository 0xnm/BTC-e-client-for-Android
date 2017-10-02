package com.QuarkLabs.BTCeClient.ui.chat;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.R;
import com.QuarkLabs.BTCeClient.WexLocale;

import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<ChatMessage>> {

    private static final String LANGUAGE_DIALOG_SHOWN_KEY = "LANGUAGE_DIALOG_SHOWN";
    private static final String IS_LOADING_KEY = "IS_LOADING";


    @SuppressWarnings("NullableProblems")
    @NonNull
    private AppPreferences appPreferences;
    private RecyclerView chatsView;
    private ChatAdapter chatAdapter;
    private View errorView;
    private View progressView;

    private boolean isContentLoading;
    @Nullable
    private AlertDialog languageDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        appPreferences = BtcEApplication.get(getActivity()).getAppPreferences();
        chatsView = (RecyclerView) view.findViewById(R.id.chat_content);
        errorView = view.findViewById(R.id.chat_error);
        progressView = view.findViewById(R.id.chat_loading);

        setHasOptionsMenu(true);

        chatAdapter = new ChatAdapter();
        chatAdapter.setLinkify(appPreferences.isLinkifyChat());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);
        chatsView.setLayoutManager(layoutManager);
        chatsView.setAdapter(chatAdapter);

        List<ChatMessage> oldMessages = BtcEApplication.get(getActivity())
                .getInMemoryStorage().getChatMessages();
        if (oldMessages == null) {
            showLoading();
        } else {
            showContent(oldMessages);
        }
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(LANGUAGE_DIALOG_SHOWN_KEY, false)) {
                showLanguageSelectionDialog();
            }
            isContentLoading = savedInstanceState.getBoolean(IS_LOADING_KEY, false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        if (isContentLoading) {
            refreshItem.setActionView(R.layout.progress_bar_action_view);
        } else {
            refreshItem.setActionView(null);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean consumed = false;
        switch (item.getItemId()) {
            case R.id.action_language:
                showLanguageSelectionDialog();
                consumed = true;
                break;
            case R.id.action_refresh:
                consumed = true;
                refreshChat(false);
                break;
        }
        return consumed || super.onOptionsItemSelected(item);
    }

    private void refreshChat(boolean silent) {
        if (!silent) {
            isContentLoading = true;
            getActivity().invalidateOptionsMenu();
            if (chatsView.getVisibility() != View.VISIBLE) {
                showLoading();
            }
        }
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (languageDialog != null) {
            outState.putBoolean(LANGUAGE_DIALOG_SHOWN_KEY, true);
        }
    }

    private void showLanguageSelectionDialog() {
        String[] choiceItems = getResources().getStringArray(R.array.languages);
        @WexLocale final String currentChatLocale = appPreferences.getChatLocale();
        int checkedItem;
        if (WexLocale.EN.equals(currentChatLocale)) {
            checkedItem = 0;
        } else if (WexLocale.RU.equals(currentChatLocale)) {
            checkedItem = 1;
        } else if (WexLocale.CN.equals(currentChatLocale)) {
            checkedItem = 2;
        } else {
            throw new RuntimeException("Unknown chat locale: " + currentChatLocale);
        }
        languageDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.language)
                .setSingleChoiceItems(choiceItems, checkedItem,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handleLanguageItemSelection(which);
                            }
                        })
                .show();
    }

    private void handleLanguageItemSelection(int which) {
        @WexLocale String currentChatLocale = appPreferences.getChatLocale();
        @WexLocale String selectedItem = null;
        switch (which) {
            case 0:
                selectedItem = WexLocale.EN;
                break;
            case 1:
                selectedItem = WexLocale.RU;
                break;
            case 2:
                selectedItem = WexLocale.CN;
                break;
            default:
                break;
        }
        //noinspection ConstantConditions
        if (!selectedItem.equals(currentChatLocale)) {
            BtcEApplication.get(getActivity()).getInMemoryStorage()
                    .setChatMessages(Collections.<ChatMessage>emptyList());
            appPreferences.setChatLocale(selectedItem);
            showLoading();
            getLoaderManager().restartLoader(0, null, ChatFragment.this);
        }

        if (languageDialog != null) {
            languageDialog.dismiss();
            languageDialog = null;
        }
    }

    private void showContent(@NonNull List<ChatMessage> messages) {
        chatAdapter.setMessages(messages);

        chatsView.setVisibility(View.VISIBLE);
        progressView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);

        // yeah, magic number, quick and dirty solution instead of going for data observers
        chatsView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible()
                        && chatsView != null && chatAdapter != null) {
                    chatsView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                }
            }
        }, 300);
    }

    private void showLoading() {
        chatsView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void showError() {
        chatsView.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    public Loader<List<ChatMessage>> onCreateLoader(int id, Bundle args) {
        return new ChatLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ChatMessage>> loader, List<ChatMessage> messages) {
        isContentLoading = false;
        if (messages.isEmpty()) {
            showError();
        } else {
            BtcEApplication.get(getActivity()).getInMemoryStorage().setChatMessages(messages);
            showContent(messages);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<List<ChatMessage>> loader) {
        // not interested
    }
}
