package com.ringly.ringly.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.ringly.ringly.R;
import com.ringly.ringly.RinglyApp;

import rx.functions.Func1;

/**
 * MindfulnessSessionEvent
 * It's not been used yet.
 */
public class MindfulnessSessionEvent implements BaseColumns {
    public enum MindfulnessType {
        BREATHING(RinglyApp.getInstance().getString(R.string.breathing_exercises)),
        GUIDED_AUDIO(RinglyApp.getInstance().getString(R.string.guided_meditation));

        private final String descrip;

        MindfulnessType(String descrip){
            this.descrip = descrip;
        }

        public String getDescrip() {
            return descrip;
        }
    }

    static final String TABLE = "mindfulness_session_event";

    static final String COLUMN_START_TIMESTAMP = "startTimestamp";
    static final String COLUMN_MINUTE_COUNT = "minuteCount";
    static final String COLUMN_MINDFULNESS_TYPE = "mindfulnessType";

    public final long id;
    public final long startTimestamp;
    public final int minuteCount;
    public final MindfulnessType mindfulnessType;

    public static final Func1<Cursor, MindfulnessSessionEvent> MAPPER = new Func1<Cursor, MindfulnessSessionEvent>() {
        @Override
        public MindfulnessSessionEvent call(Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(_ID));
            long startTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIMESTAMP));
            int minuteCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE_COUNT));
            int mindfulnessType = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINDFULNESS_TYPE));
            return new MindfulnessSessionEvent(id, startTimestamp, minuteCount, mindfulnessType);
        }
    };

    public MindfulnessSessionEvent(long id, long startTimestamp, int minuteCount, int mindfulnessType) {
        this.id = id;
        this.startTimestamp = startTimestamp;
        this.minuteCount = minuteCount;
        if (mindfulnessType == 0 || mindfulnessType == 1) {
            this.mindfulnessType = MindfulnessType.values()[mindfulnessType];
        } else {
            //this should never happen
            this.mindfulnessType = MindfulnessType.BREATHING;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MindfulnessSessionEvent stepEvent = (MindfulnessSessionEvent) o;

        if (id != stepEvent.id) return false;
        if (startTimestamp != stepEvent.startTimestamp) return false;
        if (minuteCount != stepEvent.minuteCount) return false;
        return mindfulnessType != null ? mindfulnessType.equals(stepEvent.mindfulnessType) : stepEvent.mindfulnessType == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + Long.valueOf(startTimestamp).hashCode();
        result = 31 * result + minuteCount;
        result = 31 * result + (mindfulnessType != null ? mindfulnessType.hashCode() : 0);
        return (int)result;
    }

    static final class Builder {
        private final ContentValues values = new ContentValues();

        public Builder id(long id) {
            values.put(_ID, id);
            return this;
        }

        public Builder startTimestamp(long startTimestamp) {
            values.put(COLUMN_START_TIMESTAMP, startTimestamp);
            return this;
        }

        public Builder minuteCount(int minute_count) {
            values.put(COLUMN_MINUTE_COUNT, minute_count);
            return this;
        }

        public Builder mindfulnessType(MindfulnessType mindfulnessType) {
            values.put(COLUMN_MINDFULNESS_TYPE, mindfulnessType.ordinal());
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}
