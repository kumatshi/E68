package com.example.e68.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SessionManager {
    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "auth_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "SessionManager initialized");
    }

    public void saveLoginSession(String userId, String role) {
        Log.d(TAG, "saveLoginSession called - userId: " + userId + ", role: " + role);
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "saveLoginSession: userId is null or empty!");
            return;
        }
        if (role == null || role.isEmpty()) {
            Log.e(TAG, "saveLoginSession: role is null or empty, setting default INSPECTOR");
            role = "INSPECTOR";
        }

        boolean success = prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_ROLE, role)
                .commit(); // Используем commit для немедленного сохранения

        Log.d(TAG, "Session saved successfully: " + success);
        Log.d(TAG, "Verification - isLoggedIn: " + isLoggedIn() + ", role: " + getUserRole());
    }

    public void clearSession() {
        Log.d(TAG, "clearSession called");
        prefs.edit().clear().commit();
        Log.d(TAG, "Session cleared");
    }

    public boolean isLoggedIn() {
        boolean result = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "isLoggedIn: " + result);
        return result;
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserRole() {
        String role = prefs.getString(KEY_USER_ROLE, null);
        Log.d(TAG, "getUserRole: " + role);
        return role;
    }
}