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

/**
 * Helper on top of SQLite storage
 */
public final class DBWorker extends SQLiteOpenHelper {
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
    public static final String NOTIFIERS_PAIR_COLUMN = "Pair";
    public static final String NOTIFIERS_TYPE_COLUMN = "Type";
    public static final String NOTIFIERS_VALUE_COLUMN = "Value";
    private static final String NOTIFIERS_TABLE_CREATE = "CREATE TABLE "
            + NOTIFIERS_DATA_TABLE_NAME
            + " (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
            + NOTIFIERS_PAIR_COLUMN + " TEXT NOT NULL, "
            + NOTIFIERS_TYPE_COLUMN + " INTEGER NOT NULL, "
            + NOTIFIERS_VALUE_COLUMN + " FLOAT NOT NULL)";

    private static volatile DBWorker sInstance;

    private DBWorker(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    public static DBWorker getInstance(Context context) {
        DBWorker localInstance = sInstance;
        if (localInstance == null) {
            synchronized (DBWorker.class) {
                localInstance = sInstance;
                if (localInstance == null) {
                    sInstance = new DBWorker(context.getApplicationContext());
                    localInstance = sInstance;
                }
            }
        }
        return localInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(NOTIFIERS_TABLE_CREATE);
        db.execSQL(TICKER_TABLE_CREATE);
        db.execSQL(WIDGET_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // not interested
    }

    public Cursor getNotifiers() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + NOTIFIERS_DATA_TABLE_NAME, null);
    }

    public void addNewNotifier(int type, String pair, float value) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues(3);
            contentValues.put("Value", value);
            contentValues.put("Pair", pair);
            contentValues.put("Type", type);
            db.insert("Notifiers", null, contentValues);
        }
    }

    public void removeNotifier(int id) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete("Notifiers", "_id=" + id, null);
        }
    }


    public long insertToWidgetData(ContentValues contentValues) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            return db.insert(WIDGET_DATA_TABLE_NAME, null, contentValues);
        }
    }

    public int updateWidgetData(ContentValues contentValues, String pair) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            String[] pairValue = {pair};
            return db.update(WIDGET_DATA_TABLE_NAME, contentValues, "pair == ?", pairValue);
        }
    }

    public Cursor pullWidgetData(String[] columns) {
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            return db.query(WIDGET_DATA_TABLE_NAME, columns, null, null, null, null, null);
        }
    }
}