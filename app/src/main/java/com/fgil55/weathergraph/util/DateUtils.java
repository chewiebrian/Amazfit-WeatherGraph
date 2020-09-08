package com.fgil55.weathergraph.util;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

public class DateUtils {

    public static long getMillis(LocalDateTime date) {
        return date.toDateTime(DateTimeZone.UTC).getMillis();
    }

    public static int getHoursOfDifference(LocalDateTime aDate, LocalDateTime anotherDate) {
        return (int) ((getMillis(aDate) - getMillis(anotherDate)) / 3600000);
    }

    public static int getDaysOfDifference(LocalDateTime aDate, LocalDateTime anotherDate) {
        return (int) ((getMillis(aDate) - getMillis(anotherDate)) / (3600000 * 24));
    }
}
