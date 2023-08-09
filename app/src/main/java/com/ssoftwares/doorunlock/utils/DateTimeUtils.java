package com.ssoftwares.doorunlock.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeUtils {

    // Method to combine date and time into ISO format
    public static String combineDateTime(String dateStr, String timeStr) {
        try {
            // Create date and time formats to parse the received strings
            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss", Locale.getDefault());

            // Parse the date and time strings to Date objects
            Date date = dateFormat.parse(dateStr);
            Date time = timeFormat.parse(timeStr);

            // Combine date and time into a single Date object
            Date combinedDateTime = combineDateAndTime(date, time);

            // Create ISO date format and set UTC time zone
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Format the combined date and time into ISO date format
            return isoFormat.format(combinedDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String combineDateTime(String inputDateTime) {
        int day = Integer.parseInt(inputDateTime.substring(0, 2));
        int month = Integer.parseInt(inputDateTime.substring(2, 4));
        int year = Integer.parseInt(inputDateTime.substring(4, 8));
        int hour = Integer.parseInt(inputDateTime.substring(8, 10));
        int minute = Integer.parseInt(inputDateTime.substring(10, 12));
        int second = Integer.parseInt(inputDateTime.substring(12, 14));

        Calendar calendar = Calendar.getInstance();
        calendar.set(year , month , day , hour , minute , second);

        // Create ISO date format and set UTC time zone
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Format the combined date and time into ISO date format
        return isoFormat.format(calendar.getTime());

    }

    private static String getDate(){
        SimpleDateFormat format =new SimpleDateFormat("ddMMyyyy");
        Date date = new Date(System.currentTimeMillis());

        return format.format(date);
    }

    private static String getTime(){
        SimpleDateFormat format =new SimpleDateFormat("HHmmss");
        Date date = new Date(System.currentTimeMillis());

        return format.format(date);
    }
    // Method to combine Date and Time into a single Date object
    private static Date combineDateAndTime(Date date, Date time) {
        if (date == null || time == null) return null;

        long dateTimeMillis = date.getTime() + time.getTime();
        return new Date(dateTimeMillis);
    }
}
