package com.ringly.ringly.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import rx.functions.Func1;

public class StepEvent implements BaseColumns {
    static final String TABLE = "step_event";

    static final String COLUMN_TIME = "time";
    static final String COLUMN_WALKING = "walking";
    static final String COLUMN_RUNNING = "running";
    static final String COLUMN_MAC_ADDRESS = "mac";

    public final long id;
    public final Date date;
    public final int walking;
    public final int running;
    public final String mac;

    public static final Func1<Cursor, StepEvent> MAPPER = new Func1<Cursor, StepEvent>() {
        @Override
        public StepEvent call(Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
            int walking = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WALKING));
            int running = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RUNNING));
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIME)));
            cal.setTimeZone(TimeZone.getDefault());
            String mac = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MAC_ADDRESS));
            return new StepEvent(id, cal.getTime(), walking, running, mac);
        }
    };

    public StepEvent(long id, Date date, int walking, int running, String mac) {
        this.id = id;
        this.date = date;
        this.walking = walking;
        this.running = running;
        this.mac = mac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StepEvent stepEvent = (StepEvent) o;

        if (id != stepEvent.id) return false;
        if (walking != stepEvent.walking) return false;
        if (running != stepEvent.running) return false;
        if (date != null ? !date.equals(stepEvent.date) : stepEvent.date != null) return false;
        return mac != null ? mac.equals(stepEvent.mac) : stepEvent.mac == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + walking;
        result = 31 * result + running;
        result = 31 * result + (mac != null ? mac.hashCode() : 0);
        return result;
    }

    static final class Builder {
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(_ID, id);
            return this;
        }

        public Builder time(Date date) {
            values.put(COLUMN_TIME, date.getTime());
            return this;
        }

        public Builder walking(int steps) {
            values.put(COLUMN_WALKING, steps);
            return this;
        }

        public Builder running(int steps) {
            values.put(COLUMN_RUNNING, steps);
            return this;
        }

        public Builder mac(String mac) {
            values.put(COLUMN_MAC_ADDRESS, mac);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}
