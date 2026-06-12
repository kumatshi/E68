package com.example.e68.app.service;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class FcmNotificationSender {

    private static final String TAG = "FcmSender";

    // Типы уведомлений
    public static final String TYPE_NEW_DEFECT = "new_defect";
    public static final String TYPE_CRITICAL = "critical_defect";
    public static final String TYPE_ASSIGNED = "assigned";
    public static final String TYPE_STATUS_CHANGED = "status_changed";
    public static final String TYPE_RESOLVED = "resolved";
    public static final String TYPE_DAILY = "daily_digest";
    public static final String TYPE_WEEKLY = "weekly_report";

    /**
     * Отправка уведомления конкретному пользователю по UID
     */
    public static void sendToUser(String userId, String type, String title, String body) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String token = doc.getString("fcmToken");
                    if (token != null && !token.isEmpty()) {
                        sendViaHttpApi(token, type, title, body);
                    } else {
                        Log.d(TAG, "User " + userId + " has no FCM token");
                    }
                });
    }

    /**
     * Отправка всем пользователям с определённой ролью
     */
    public static void sendToRole(String role, String type, String title, String body) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("role", role)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String token = doc.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            sendViaHttpApi(token, type, title, body);
                        }
                    }
                });
    }

    /**
     * Отправка через HTTP API (требуется Server Key)
     * Для работы нужно добавить Server Key из Firebase Console
     */
    private static void sendViaHttpApi(String token, String type, String title, String body) {
        // TODO: Реализовать через OkHttp или Retrofit
        // Пока просто логируем
        Log.d(TAG, "Sending to " + token + ": " + title + " - " + body);
    }

    /**
     * Сохранить FCM токен пользователя в Firestore
     */
    public static void saveToken(String userId, String token) {
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("fcmTokenUpdated", System.currentTimeMillis());
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
    }
}