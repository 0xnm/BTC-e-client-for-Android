package com.QuarkLabs.BTCeClient.models;

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

public class Ticker {

    private String pair;
    private double high;
    private double low;
    private double avg;
    private double vol;
    private double volCur;
    private double last;
    private double buy;
    private double sell;
    private long updated;
    private double fee;

    public Ticker(String pair) {
        this.pair = pair;
    }

    public double getFee() {
        return fee;
    }

    public String getPair() {
        return pair;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getVol() {
        return vol;
    }

    public void setVol(double vol) {
        this.vol = vol;
    }

    public double getVolCur() {
        return volCur;
    }

    public void setVolCur(double volCur) {
        this.volCur = volCur;
    }

    public double getLast() {
        return last;
    }

    public void setLast(double last) {
        this.last = last;
        fee = last * 0.002 * 2;
    }

    public double getBuy() {
        return buy;
    }

    public void setBuy(double buy) {
        this.buy = buy;
    }

    public double getSell() {
        return sell;
    }

    public void setSell(double sell) {
        this.sell = sell;
    }

    public double getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }
}
