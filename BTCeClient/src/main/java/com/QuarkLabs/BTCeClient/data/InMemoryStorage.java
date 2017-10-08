package com.QuarkLabs.BTCeClient.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.api.Ticker;
import com.QuarkLabs.BTCeClient.ui.chat.ChatMessage;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryStorage {
    @NonNull
    private final Map<String, Ticker> latestTickers =
            Collections.synchronizedMap(new HashMap<String, Ticker>());
    @NonNull
    private final Map<String, Ticker> previousTickers =
            Collections.synchronizedMap(new HashMap<String, Ticker>());

    @Nullable
    private List<ChatMessage> chatMessages;
    @Nullable
    private Map<String, BigDecimal> funds;

    public InMemoryStorage() { }

    public void saveTickers(@NonNull Map<String, Ticker> newTickers) {
        previousTickers.clear();
        previousTickers.putAll(latestTickers);
        latestTickers.clear();
        latestTickers.putAll(newTickers);
    }

    /**
     * Provides previous data
     *
     * @return Map in format pair - ticker
     */
    @NonNull
    public Map<String, Ticker> getPreviousData() {
        return new HashMap<>(previousTickers);
    }

    /**
     * Provides latest data
     *
     * @return Map in format pair - ticker
     */
    @NonNull
    public Map<String, Ticker> getLatestData() {
        return new HashMap<>(latestTickers);
    }

    public void addNewTicker(@NonNull Ticker ticker) {
        latestTickers.put(ticker.getPair(), ticker);
    }

    public void removeTicker(@NonNull Ticker ticker) {
        latestTickers.remove(ticker.getPair());
        previousTickers.remove(ticker.getPair());
    }

    public void clearTickers() {
        latestTickers.clear();
        previousTickers.clear();
    }

    @Nullable
    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(@Nullable List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Nullable
    public Map<String, BigDecimal> getFunds() {
        return funds;
    }

    public void setFunds(@Nullable Map<String, BigDecimal> funds) {
        this.funds = funds;
    }
}
