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

package com.QuarkLabs.BTCeClient;

import com.QuarkLabs.BTCeClient.models.Ticker;

import java.util.HashMap;
import java.util.Map;

public class TickersStorage {
    private static Map<String, Ticker> mLatestTickers = new HashMap<>();
    private static Map<String, Ticker> mPreviousTickers = new HashMap<>();

    public static void saveData(Map<String, Ticker> newData) {
        mPreviousTickers = new HashMap<>(mLatestTickers);
        mLatestTickers = newData;
    }

    public static Map<String, Ticker> loadPreviousData() {
        return mPreviousTickers;
    }

    public static Map<String, Ticker> loadLatestData() {
        return mLatestTickers;
    }

    public static void addNewTicker(Ticker ticker) {
        mLatestTickers.put(ticker.getPair(), ticker);
    }

}
