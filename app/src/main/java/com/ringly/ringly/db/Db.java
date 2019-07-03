package com.ringly.ringly.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.common.base.Optional;
import com.ringly.ringly.BuildConfig;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.bluetooth.Utilities;
import com.ringly.ringly.ui.screens.activity.StepsData;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

public class Db {
    public static final String TAG = Db.class.getCanonicalName();

    private static Db instance;

    private Context mContext;
    private final BriteDatabase mBriteDb;

    private List<ContentValues> mCvs;

    public static Db getInstance(final Context context) {
        return getInstance(context, "steps.db");
    }

    public static Db getInstance(final Context context, String dbName) {
        if (instance == null) {
            instance = new Db(context, dbName);
        }

        return instance;
    }

    protected Db(final Context context, String dbName) {
        mContext = context;

        SQLiteOpenHelper dbHelper = new DbOpenHelper(context, dbName);
        SqlBrite sqlBrite = SqlBrite.create(message -> Log.d(TAG, "log: " + message));
        mBriteDb = sqlBrite.wrapDatabaseHelper(dbHelper, Schedulers.io());
        mBriteDb.setLoggingEnabled(BuildConfig.LOG_DB);
    }

    ////
    //// Querying
    ////

    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM " + StepEvent.TABLE;

    public Observable<Integer> getCount() {
        return mBriteDb.createQuery(StepEvent.TABLE, COUNT_QUERY)
            .map(q -> {
                try (Cursor c = q.run()) {
                    if (c == null || !c.moveToNext()) {
                        throw new AssertionError("No rows");
                    }

                    return c.getInt(0);
                }
            });
    }

    private static final String ALL_QUERY = "SELECT * FROM " + StepEvent.TABLE;

    public Observable<List<StepEvent>> getEvents() {
        return mBriteDb.createQuery(StepEvent.TABLE, ALL_QUERY)
            .mapToList(StepEvent.MAPPER);
    }

    public Observable<List<StepsData>> eventsByField(int field) {
        return getEvents()
            .observeOn(Schedulers.computation())
            .flatMap(ses -> Observable.from(ses)
                .groupBy(se -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(se.date);
                    return com.ringly.ringly.ui.Utilities.truncateCal(cal, field).getTimeInMillis();
                })
                .flatMap(go ->
                    go.collect(StepsData.Builder::new,
                        StepsData.Builder::addStepEvent)
                        .map(b -> b.build(Preferences.getHeight(mContext),
                            Preferences.getWeight(mContext),
                            Preferences.getBirthday(mContext)
                                .transform(Calendar::getTimeInMillis),
                            true,
                            field))
                )
                .toSortedList()
            );
    }

    private static final String DEVICE_QUERY =
        "SELECT * FROM " + StepEvent.TABLE +
            " WHERE " + StepEvent.COLUMN_MAC_ADDRESS + " = ?" +
            " ORDER BY " + StepEvent.COLUMN_TIME + " DESC"  +
            " LIMIT 1";

    public Observable<Optional<StepEvent>> getLatestDeviceEvent(String mac) {
        return mBriteDb.createQuery(StepEvent.TABLE, DEVICE_QUERY, mac)
            .mapToList(StepEvent.MAPPER)
            .map(ses -> ses.size() == 0 ? Optional.absent() : Optional.of(ses.get(0)));
    }

    ////
    //// Modification
    ////

    public void recordBytes(byte[] bytes, String macAddress) {
        if(bytes.length != 5) {
            throw new IllegalArgumentException("A step event has five bytes");
        }

        // Don't record control bytes
        if (Utilities.minuteBytesToMs(bytes) == Utilities.START_DATE) {
            return;
        }

        // Don't record uninitialized bytes
        boolean initialized = false;
        for (byte b : bytes) {
            if ((b & 0xFF) != 0xFF) {
                initialized = true;
                break;
            }
        }

        long time = Utilities.minuteBytesToMs(Arrays.copyOfRange(bytes, 0, 3));

        // Sanity check - exclude records that are timestamped more than a week in the future.
        if(time - System.currentTimeMillis() > TimeUnit.DAYS.toMillis(7)) {
            return;
        }

        if (!initialized) {
            return;
        }

        mBriteDb.insert(StepEvent.TABLE, new StepEvent.Builder()
            .time(new Date(time))
            .walking(bytes[3] & 0xFF)
            .running(bytes[4] & 0xFF)
            .mac(macAddress)
            .build(), SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void clear() {
        mBriteDb.delete(StepEvent.TABLE, null);
    }

    public void addSample() {
        if(mCvs == null) {
            mCvs = getSampleData();
        }

        getCount().take(1)
            .subscribe(
                count -> mBriteDb.insert(StepEvent.TABLE, mCvs.get(count)),
                err -> Log.e(TAG, "addSample: ", err)
            );
    }

    ////
    //// Private
    ////

    private List<ContentValues> getSampleData() {
        ArrayList<ContentValues> cvs = new ArrayList<>();

        cvs.add(new StepEvent.Builder().time(new Date(1474329600000L)).walking(19).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474330200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474330800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474331400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474332000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474332600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474333200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474333800000L)).walking(176).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474334400000L)).walking(40).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474335000000L)).walking(58).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474335600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474336200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474336800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474337400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474338000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474338600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474339200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474339800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474340400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474341000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474341600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474342200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474342800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474343400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474344000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474344600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474345200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474345800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474346400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474347000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474347600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474348200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474348800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474349400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474350000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474350600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474351200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474351800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474352400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474353000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474353600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474354200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474354800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474355400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474356000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474356600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474357200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474357800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474358400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474359000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474359600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474360200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474360800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474361400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474362000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474362600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474363200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474363800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474364400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474365000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474365600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474366200000L)).walking(222).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474366800000L)).walking(878).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474367400000L)).walking(96).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474368000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474368600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474369200000L)).walking(34).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474369800000L)).walking(40).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474370400000L)).walking(5).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474371000000L)).walking(43).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474371600000L)).walking(43).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474372200000L)).walking(118).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474372800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474373400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474374000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474374600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474375200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474375800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474376400000L)).walking(144).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474377000000L)).walking(515).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474377600000L)).walking(749).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474378200000L)).walking(119).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474378800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474379400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474380000000L)).walking(164).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474380600000L)).walking(11).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474381200000L)).walking(325).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474381800000L)).walking(6).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474382400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474383000000L)).walking(37).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474383600000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474384200000L)).walking(81).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474384800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474385400000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474386000000L)).walking(8).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474386600000L)).walking(332).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474387200000L)).walking(988).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474387800000L)).walking(333).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474388400000L)).walking(570).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474389000000L)).walking(3).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474389600000L)).walking(22).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474390200000L)).walking(503).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474390800000L)).walking(644).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474391400000L)).walking(473).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474392000000L)).walking(206).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474392600000L)).walking(30).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474393200000L)).walking(62).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474393800000L)).walking(245).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474394400000L)).walking(48).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474395000000L)).walking(276).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474395600000L)).walking(168).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474396200000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474396800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474397400000L)).walking(82).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474398000000L)).walking(151).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474398600000L)).walking(596).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474399200000L)).walking(79).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474399800000L)).walking(242).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474400400000L)).walking(55).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474401000000L)).walking(361).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474401600000L)).walking(435).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474402200000L)).walking(257).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474402800000L)).walking(43).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474403400000L)).walking(95).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474404000000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474404600000L)).walking(20).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474405200000L)).walking(8).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474405800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474406400000L)).walking(509).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474407000000L)).walking(646).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474407600000L)).walking(730).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474408200000L)).walking(683).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474408800000L)).walking(976).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474409400000L)).walking(1170).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474410000000L)).walking(158).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474410600000L)).walking(340).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474411200000L)).walking(501).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474411800000L)).walking(599).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474412400000L)).walking(35).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474413000000L)).walking(21).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474413600000L)).walking(404).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474414200000L)).walking(170).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474414800000L)).walking(0).mac("sample").build());
        cvs.add(new StepEvent.Builder().time(new Date(1474415400000L)).walking(0).mac("sample").build());

        return cvs;
    }
}
