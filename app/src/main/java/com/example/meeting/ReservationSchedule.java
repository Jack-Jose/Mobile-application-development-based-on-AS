package com.example.meeting;

import java.util.Calendar;

/** Generates recurrence dates in local calendar time, including daylight-saving changes. */
public final class ReservationSchedule {
    private ReservationSchedule() {}

    public static long[] starts(long firstStart, int recurrence) {
        int count = recurrence == 1 ? 7 : recurrence == 2 || recurrence == 3 ? 4 : 1;
        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(firstStart);
            if (recurrence == 1) date.add(Calendar.DAY_OF_MONTH, i);
            else if (recurrence == 2) date.add(Calendar.WEEK_OF_YEAR, i);
            else if (recurrence == 3) date.add(Calendar.MONTH, i);
            result[i] = date.getTimeInMillis();
        }
        return result;
    }
}
