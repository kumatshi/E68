package com.example.e68.app.util;

public class Constants {

    // Базовые URL
    public static final String BASE_URL = "https://api.e68.ru/";

    // Каналы уведомлений
    public static final String PATROL_CHANNEL_ID = "patrol_channel";
    public static final String DEFECT_CHANNEL_ID = "defect_channel";

    // Настройки GPS
    public static final long GPS_INTERVAL_MS = 5000;
    public static final float GPS_MIN_DISTANCE_M = 10f;

    // Статусы дефектов
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_REJECTED = "REJECTED";

    // Типы операций для SyncQueue
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
}