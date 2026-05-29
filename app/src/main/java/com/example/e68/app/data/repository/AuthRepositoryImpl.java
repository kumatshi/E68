package com.example.e68.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.domain.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepositoryImpl {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    @Inject
    public AuthRepositoryImpl() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }

    public void login(String email, String password, OnLoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    Log.d(TAG, "Firebase Auth OK, uid=" + uid + ", теперь читаем Firestore...");
                    loadUserFromFirestore(uid, email, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase Auth FAIL: " + e.getMessage());
                    callback.onError("Неверный email или пароль");
                });
    }

    private void loadUserFromFirestore(String uid, String email, OnLoginCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> data = doc.getData();
                        Log.d(TAG, "Firestore doc found: " + data);

                        User user = new User();
                        user.setUid(uid); // ВАЖНО: устанавливаем uid!
                        user.setName(getStrTrimKey(data, "name"));
                        user.setEmail(getStrTrimKey(data, "email"));
                        user.setRole(getStrTrimKey(data, "role"));
                        user.setDepartment(getStrTrimKey(data, "department"));

                        Object activeVal = getValueTrimKey(data, "isActive");
                        if (activeVal == null) activeVal = getValueTrimKey(data, "active");
                        user.setActive(Boolean.TRUE.equals(activeVal));

                        Log.d(TAG, "User loaded: uid=" + user.getUid() + ", name=" + user.getName()
                                + ", role=" + user.getRole()
                                + ", active=" + user.isActive());

                        if (!user.isActive()) {
                            callback.onError("Аккаунт заблокирован");
                            return;
                        }

                        currentUser.postValue(user);
                        callback.onSuccess(user);

                    } else {
                        Log.w(TAG, "Документ users/" + uid + " не найден. Создаём...");
                        createDefaultUser(uid, email, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore read FAIL: " + e.getMessage());
                    callback.onError("Ошибка получения данных: " + e.getMessage());
                });
    }

    private void createDefaultUser(String uid, String email, OnLoginCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid",        uid);
        data.put("name",       extractNameFromEmail(email));
        data.put("email",      email);
        data.put("role",       "INSPECTOR");
        data.put("department", "Не назначен");
        data.put("isActive",   true);

        db.collection("users").document(uid).set(data)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Создан дефолтный документ для uid=" + uid);

                    User user = new User();
                    user.setUid(uid); // ВАЖНО: устанавливаем uid!
                    user.setName(extractNameFromEmail(email));
                    user.setEmail(email);
                    user.setRole("INSPECTOR");
                    user.setDepartment("Не назначен");
                    user.setActive(true);

                    currentUser.postValue(user);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Не удалось создать документ: " + e.getMessage());
                    User user = new User();
                    user.setUid(uid); // ВАЖНО: устанавливаем uid!
                    user.setEmail(email);
                    user.setRole("INSPECTOR");
                    user.setActive(true);
                    currentUser.postValue(user);
                    callback.onSuccess(user);
                });
    }

    public void logout() {
        auth.signOut();
        currentUser.postValue(null);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // ── Хелперы ──────────────────────────────────────────────────────

    private Object getValueTrimKey(Map<String, Object> map, String key) {
        if (map == null || key == null) return null;
        if (map.containsKey(key)) return map.get(key);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (key.equals(entry.getKey().trim())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String getStrTrimKey(Map<String, Object> map, String key) {
        Object val = getValueTrimKey(map, key);
        return val != null ? val.toString().trim() : "";
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "Пользователь";
        return email.split("@")[0];
    }

    public interface OnLoginCallback {
        void onSuccess(User user);
        void onError(String message);
    }
}