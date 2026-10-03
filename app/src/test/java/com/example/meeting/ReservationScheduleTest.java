package com.example.meeting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class ReservationScheduleTest {
    @Test
    public void dailyRecurrenceKeepsLocalTimeAcrossDaylightSaving() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            Calendar first = Calendar.getInstance();
            first.set(2026, Calendar.MARCH, 7, 10, 0, 0);
            first.set(Calendar.MILLISECOND, 0);

            long[] starts = ReservationSchedule.starts(first.getTimeInMillis(), 1);
            Calendar second = Calendar.getInstance();
            second.setTimeInMillis(starts[1]);

            assertEquals(7, starts.length);
            assertEquals(23L * 60 * 60 * 1000, starts[1] - starts[0]);
            assertEquals(10, second.get(Calendar.HOUR_OF_DAY));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void overnightBookingIsOutsideBusinessHours() {
        Calendar first = Calendar.getInstance();
        first.set(2026, Calendar.OCTOBER, 3, 22, 0, 0);
        long start = first.getTimeInMillis();
        assertNull(FeeCalculator.checkBusinessHours(start, start + 60 * 60_000L));
        assertNotNull(FeeCalculator.checkBusinessHours(start, start + 3 * 60 * 60_000L));
    }
}
