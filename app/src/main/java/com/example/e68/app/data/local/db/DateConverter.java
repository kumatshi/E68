package com.example.e68.app.data.local.db;

import androidx.room.TypeConverter;
import java.util.Date;

public class DateConverter {

    @TypeConverter
    public static Long fromDate(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date toDate(Long timestamp) {
        return timestamp == null ? null : new Date(timestamp);
    }
}