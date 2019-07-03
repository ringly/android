package com.ringly.ringly;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.ringly.ringly.config.model.Height;
import com.ringly.ringly.config.model.Weight;
import com.ringly.ringly.db.Db;
import com.ringly.ringly.db.StepEvent;
import com.ringly.ringly.ui.screens.activity.StepsData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.observers.TestSubscriber;

import static com.ringly.ringly.bluetooth.Utilities.START_DATE;

@RunWith(AndroidJUnit4.class)
public class DbTest {
    private Db mDb;

    @Before
    public void createDb() {
        mDb = Db.getInstance(InstrumentationRegistry.getTargetContext(), "steps-test.db");
        mDb.clear();
    }

    @Test
    public void testDbCount() {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        mDb.getCount().take(3).subscribe(testSubscriber);

        mDb.addSample();
        testSubscriber.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        mDb.addSample();

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertReceivedOnNext(Arrays.asList(0, 1, 2));
    }

    @Test
    public void  testRecordBytes() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(2).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{ 1, 0, 0, 1, 2 }, "test");
        testSubscriber.awaitTerminalEvent(100, TimeUnit.MILLISECONDS);
        mDb.recordBytes(new byte[]{ 0, 2, 0, 3, 4 }, "test");

        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 1, 2, "test"));
        List<StepEvent> data2 = new ArrayList<>(data);
        data2.add(new StepEvent(2, new Date(START_DATE + TimeUnit.MINUTES.toMillis(2 * 256)), 3, 4, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(data, data2));
    }

    /**
     * We only want one record per timestamp
     */
    @Test
    public void testDuplicateTimeRecord() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{ 1, 0, 0, 1, 2 }, "test");
        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 1, 2, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(data);

        testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{ 1, 0, 0, 3, 4 }, "test");

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(data);
    }

    @Test
    public void testTopBit() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{(byte) 0x01, 0x00, (byte) 0x80, 0x01, 0x02 }, "test");

        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 1, 2, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(data));
    }

    @Test
    public void testUninitializedMemory() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{(byte) 0x01, 0x00, 0x00, 0x01, 0x02 }, "test");
        mDb.recordBytes(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, "test");

        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 1, 2, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(data));
    }

    @Test
    public void testUnsignedByte() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{(byte) 0x01, 0x00, 0x00, (byte) 0x96, 0x02 }, "test");

        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 150, 2, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(data));
    }

    @Test
    public void testControlByte() {
        TestSubscriber<List<StepEvent>> testSubscriber = new TestSubscriber<>();
        mDb.getEvents().debounce(100, TimeUnit.MILLISECONDS).take(1).subscribe(testSubscriber);

        mDb.recordBytes(new byte[]{(byte) 0x01, 0x00, 0x00, 0x01, 0x02 }, "test");
        mDb.recordBytes(new byte[]{0x00, 0x00, 0x00, 0x00, 0x02 }, "test");

        Date date = new Date(START_DATE + TimeUnit.MINUTES.toMillis(1));
        List<StepEvent> data = Arrays.asList(new StepEvent(1, date, 1, 2, "test"));
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(data));
    }

    @Test
    public void testEventsByDay() {
        Preferences.setHeight(InstrumentationRegistry.getTargetContext(), new Height(150, Height.HeightUnit.CM));
        Preferences.setWeight(InstrumentationRegistry.getTargetContext(), new Weight(1, Weight.WeightUnit.KG));

        TestSubscriber<List<StepsData>> testSubscriber = new TestSubscriber<>();

        // Fill with two days of events
        for (int j = 0; j < 2; ++j) {
            for (int i = 0; i < 24 * 60; ++i) {
                int mins = j * (24 * 60) + i;
                mDb.recordBytes(new byte[]{(byte) (mins % 256), (byte) (mins / 256), 0, 1, 2}, "test");
            }
        }

        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(START_DATE + TimeUnit.MINUTES.toMillis(1));
        int minsInEndDay = (startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE));
        int minsInStartDay = 24 * 60 - minsInEndDay;
        Calendar endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.MINUTE, minsInStartDay - 1);
        StepsData sd =
            new StepsData(startDate.getTime(), endDate.getTime(),
                minsInStartDay + 2 * minsInStartDay,
                (int) (0.415 * 1.5 * (1.2 * minsInStartDay + 1.5 * 2 * minsInStartDay)),
                0.415 * 1.5 * (minsInStartDay + 2 * minsInStartDay), minsInStartDay);

        startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.MINUTE, 1);
        endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.MINUTE, 24 * 60 - 1);
        StepsData sd2 =
            new StepsData(startDate.getTime(), endDate.getTime(),
                24 * 60 + 24 * 60 * 2,
                (int) (0.415 * 1.5 * (1.2 * 24 * 60 + 1.5 * 24 * 60 * 2)),
                0.415 * 1.5 * (24 * 60 + 24 * 60 * 2), 24 * 60);

        startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.MINUTE, 1);
        minsInEndDay -= 1;
        endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.MINUTE, minsInEndDay - 1);
        StepsData sd3 =
            new StepsData(startDate.getTime(), endDate.getTime(),
                minsInEndDay + 2 * minsInEndDay,
                (int) (0.415 * 1.5 * (1.2 * minsInEndDay + 1.5 * 2 * minsInEndDay)),
                0.415 * 1.5 * (minsInEndDay + 2 * minsInEndDay), minsInEndDay);

        mDb.eventsByField(Calendar.DAY_OF_MONTH)
            .debounce(100, TimeUnit.MILLISECONDS)
            .take(1)
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(Arrays.asList(sd, sd2, sd3));
    }

}
