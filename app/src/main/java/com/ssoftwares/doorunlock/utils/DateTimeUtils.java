package com.ssoftwares.doorunlock.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtils {

    // Method to combine date and time into ISO format
    public static String combineDateTime(String dateStr, String timeStr) {
        try {
            // Create date and time formats to parse the received strings
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss", Locale.US);

            // Parse the date and time strings to Date objects
            Date date = dateFormat.parse(dateStr);
            Date time = timeFormat.parse(timeStr);

            // Combine date and time into a single Date object
            Date combinedDateTime = combineDateAndTime(date, time);

            // Create ISO date format and set UTC time zone
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Format the combined date and time into ISO date format
            return isoFormat.format(combinedDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Method to combine Date and Time into a single Date object
    private static Date combineDateAndTime(Date date, Date time) {
        if (date == null || time == null) return null;

        long dateTimeMillis = date.getTime() + time.getTime();
        return new Date(dateTimeMillis);
    }
}
