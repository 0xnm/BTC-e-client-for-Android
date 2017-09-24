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

package com.QuarkLabs.BTCeClient;

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.api.Ticker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TickersStorage {
    @NonNull
    private final Map<String, Ticker> latestTickers =
            Collections.synchronizedMap(new HashMap<String, Ticker>());
    @NonNull
    private final Map<String, Ticker> previousTickers =
            Collections.synchronizedMap(new HashMap<String, Ticker>());

    TickersStorage() { }

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

}
