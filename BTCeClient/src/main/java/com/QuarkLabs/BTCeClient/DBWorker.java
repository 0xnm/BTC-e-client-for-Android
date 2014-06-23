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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBWorker extends SQLiteOpenHelper {
    private static final String DB_NAME = "data.sqlite";
    private static final int DATABASE_VERSION = 1;
    private static final String TICKER_DATA_TABLE_NAME = "ticker_data";
    private static final String TICKER_TABLE_CREATE = "CREATE TABLE " + TICKER_DATA_TABLE_NAME +
            " (pair TEXT NOT NULL, last REAL NOT NULL DEFAULT 0.0, " +
            "sell REAL NOT NULL DEFAULT 0.0, buy REAL NOT NULL DEFAULT 0.0)";
    private static final String WIDGET_DATA_TABLE_NAME = "widget_data";
    private static final String WIDGET_TABLE_CREATE = "CREATE TABLE " + WIDGET_DATA_TABLE_NAME +
            " (pair TEXT NOT NULL, last REAL NOT NULL DEFAULT 0.0, " +
            "sell REAL NOT NULL DEFAULT 0.0, buy REAL NOT NULL DEFAULT 0.0)";
    private static final String NOTIFIERS_DATA_TABLE_NAME = "Notifiers";
    private static final String NOTIFIERS_TABLE_CREATE = "CREATE TABLE " + NOTIFIERS_DATA_TABLE_NAME +
            " (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Pair TEXT NOT NULL, " +
            "Type INTEGER NOT NULL, Value FLOAT NOT NULL)";
    private static DBWorker sInstance;

    private DBWorker(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    public static DBWorker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DBWorker(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(NOTIFIERS_TABLE_CREATE);
        db.execSQL(TICKER_TABLE_CREATE);
        db.execSQL(WIDGET_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public Cursor getNotifiers() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NOTIFIERS_DATA_TABLE_NAME, null);
    }

    public synchronized void addNewNotifier(int type, String pair, float value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues(3);
        contentValues.put("Value", value);
        contentValues.put("Pair", pair);
        contentValues.put("Type", type);
        db.insert("Notifiers", null, contentValues);
    }

    public synchronized void removeNotifier(int _id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("Notifiers", "_id=" + _id, null);
    }


    public synchronized long insertToWidgetData(ContentValues contentValues) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(WIDGET_DATA_TABLE_NAME, null, contentValues);
    }

    public synchronized int updateWidgetData(ContentValues contentValues, String pair) {
        SQLiteDatabase db = getWritableDatabase();
        String[] pairValue = {pair};
        return db.update(WIDGET_DATA_TABLE_NAME, contentValues, "pair == ?", pairValue);
    }


    public synchronized Cursor pullWidgetData(String[] columns) {
        SQLiteDatabase db = getWritableDatabase();
        return db.query(WIDGET_DATA_TABLE_NAME, columns, null, null, null, null, null);
    }
}