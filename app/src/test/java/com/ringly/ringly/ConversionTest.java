package com.ringly.ringly;

import com.ringly.ringly.bluetooth.Utilities;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConversionTest {
    @org.junit.Test
    public void testByteToMinuteConversion() {
        long ms = Utilities.START_DATE + TimeUnit.MINUTES.toMillis(1);
        assertThat(com.ringly.ringly.bluetooth.Utilities.minuteBytesToMs(new byte[]{1, 0, 0}), is(ms));

        Date date = new Date();
        date.setTime(Utilities.START_DATE);
        date.setMinutes(16);
        date.setHours(8);
        date.setDate(22);
        date.setMonth(4);
        date.setYear(116);
        date.setSeconds(0);
        assertThat(com.ringly.ringly.bluetooth.Utilities.minuteBytesToMs(new byte[]{0, 0, 1}),
            is(date.getTime() - TimeUnit.MINUTES.toMillis(date.getTimezoneOffset())));
    }
}
