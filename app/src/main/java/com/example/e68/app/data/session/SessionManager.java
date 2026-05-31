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
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "SessionManager initialized");
    }

    // Сохранение сессии после успешного входа
    public void saveSession(String userId, String email, String role, String name) {
        Log.d(TAG, "saveSession: userId=" + userId + ", email=" + email + ", role=" + role);
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_ROLE, role)
                .putString(KEY_USER_NAME, name)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // ★ ДОБАВЛЕНО: сохранение сессии (совместимость с существующим кодом)
    public void saveLoginSession(String userId, String role) {
        Log.d(TAG, "saveLoginSession: userId=" + userId + ", role=" + role);
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_ROLE, role)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Очистка сессии при выходе
    public void clearSession() {
        Log.d(TAG, "clearSession");
        prefs.edit().clear().apply();
    }

    // Проверка авторизован ли пользователь
    public boolean isLoggedIn() {
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "isLoggedIn: " + isLoggedIn);
        return isLoggedIn;
    }

    // Получение данных пользователя
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserRole() {
        String role = prefs.getString(KEY_USER_ROLE, "INSPECTOR");
        Log.d(TAG, "getUserRole: " + role);
        return role;
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }
}