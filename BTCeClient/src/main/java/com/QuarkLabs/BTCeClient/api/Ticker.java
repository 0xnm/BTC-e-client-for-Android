package com.QuarkLabs.BTCeClient.api;

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

import android.support.annotation.NonNull;

import com.QuarkLabs.BTCeClient.PairUtils;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

public class Ticker {

    private String pair;
    private BigDecimal high = BigDecimal.ZERO;
    private BigDecimal low = BigDecimal.ZERO;
    private BigDecimal avg = BigDecimal.ZERO;
    private BigDecimal vol = BigDecimal.ZERO;
    private BigDecimal volCur = BigDecimal.ZERO;
    private BigDecimal last = BigDecimal.ZERO;
    private BigDecimal buy = BigDecimal.ZERO;
    private BigDecimal sell = BigDecimal.ZERO;
    private long updated;
    private BigDecimal fee = BigDecimal.ZERO;

    public Ticker(String pair) {
        this.pair = pair;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public String getPair() {
        return pair;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getAvg() {
        return avg;
    }

    public void setAvg(BigDecimal avg) {
        this.avg = avg;
    }

    public BigDecimal getVol() {
        return vol;
    }

    public void setVol(BigDecimal vol) {
        this.vol = vol;
    }

    public BigDecimal getVolCur() {
        return volCur;
    }

    public void setVolCur(BigDecimal volCur) {
        this.volCur = volCur;
    }

    public BigDecimal getLast() {
        return last;
    }

    public void setLast(BigDecimal last) {
        this.last = last;
        //TODO check this
        fee = last.multiply(new BigDecimal("0.004"));
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public void setBuy(BigDecimal buy) {
        this.buy = buy;
    }

    public BigDecimal getSell() {
        return sell;
    }

    public void setSell(BigDecimal sell) {
        this.sell = sell;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    @NonNull
    public static Ticker createFromServer(@NonNull String pair, @NonNull JsonObject pairData) {
        Ticker ticker = new Ticker(PairUtils.serverToLocal(pair));
        ticker.setUpdated(pairData.get("updated").getAsLong());
        ticker.setAvg(pairData.get("avg").getAsBigDecimal().stripTrailingZeros());
        ticker.setBuy(pairData.get("buy").getAsBigDecimal().stripTrailingZeros());
        ticker.setSell(pairData.get("sell").getAsBigDecimal().stripTrailingZeros());
        ticker.setHigh(pairData.get("high").getAsBigDecimal().stripTrailingZeros());
        ticker.setLast(pairData.get("last").getAsBigDecimal().stripTrailingZeros());
        ticker.setLow(pairData.get("low").getAsBigDecimal().stripTrailingZeros());
        ticker.setVol(pairData.get("vol").getAsBigDecimal().stripTrailingZeros());
        ticker.setVolCur(pairData.get("vol_cur").getAsBigDecimal().stripTrailingZeros());
        return ticker;
    }
}
