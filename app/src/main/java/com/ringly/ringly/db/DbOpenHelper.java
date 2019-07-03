package com.ringly.ringly.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbOpenHelper extends SQLiteOpenHelper {

    public static final int VERSION = 1;

    public static final String CREATE_STEP_EVENT = ""
        + "CREATE TABLE " + StepEvent.TABLE + "("
        + StepEvent._ID + " INTEGER NOT NULL PRIMARY KEY,"
        + StepEvent.COLUMN_TIME + " INTEGER NOT NULL UNIQUE,"
        + StepEvent.COLUMN_WALKING + " INTEGER NOT NULL DEFAULT 0,"
        + StepEvent.COLUMN_RUNNING + " INTEGER NOT NULL DEFAULT 0,"
        + StepEvent.COLUMN_MAC_ADDRESS + " TEXT NOT NULL"
        + ")";


    DbOpenHelper(Context context, String dbName) {
        super(context, dbName, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_STEP_EVENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
