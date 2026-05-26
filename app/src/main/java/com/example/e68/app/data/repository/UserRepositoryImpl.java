package com.example.e68.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.domain.entity.User;
import com.example.e68.app.domain.repository.UserRepository;
import com.example.e68.app.util.Resource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserRepositoryImpl implements UserRepository {

    private static final String TAG = "UserRepository";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<User>> allUsers = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public UserRepositoryImpl() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            loadCurrentUser(auth.getCurrentUser().getUid());
        }

        auth.addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() == null) {
                currentUser.postValue(null);
            } else {
                loadCurrentUser(firebaseAuth.getCurrentUser().getUid());
            }
        });
    }

    @Override
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    private void loadCurrentUser(String uid) {
        db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser.postValue(documentSnapshotToUser(documentSnapshot));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading current user: " + e.getMessage()));
    }

    @Override
    public LiveData<List<User>> getAllUsers() {
        loadAllUsers();
        return allUsers;
    }

    private void loadAllUsers() {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = documentSnapshotToUser(doc);
                        if (user != null) users.add(user);
                    }
                    allUsers.postValue(users);
                    Log.d(TAG, "Loaded " + users.size() + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users: " + e.getMessage());
                    allUsers.postValue(new ArrayList<>());
                });
    }

    @Override
    public void createUser(String email, String password, String name, String role, String department, OnUserCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("email", email);
                    userData.put("name", name != null ? name : extractNameFromEmail(email));
                    userData.put("role", role != null ? role.toUpperCase() : "INSPECTOR");
                    userData.put("department", department != null ? department : "Не назначен");
                    userData.put("isActive", true);
                    userData.put("createdAt", System.currentTimeMillis());

                    db.collection(USERS_COLLECTION).document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User created successfully: " + uid);
                                loadAllUsers();
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> callback.onError("Ошибка создания документа: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Ошибка создания пользователя: " + e.getMessage()));
    }

    @Override
    public void updateUser(String uid, String name, String role, String department, boolean isActive, OnUserCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        if (name != null) updates.put("name", name);
        if (role != null) updates.put("role", role.toUpperCase());
        if (department != null) updates.put("department", department);
        updates.put("isActive", isActive);

        db.collection(USERS_COLLECTION).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated successfully: " + uid);
                    if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(uid)) {
                        loadCurrentUser(uid);
                    }
                    loadAllUsers();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Ошибка обновления: " + e.getMessage()));
    }

    @Override
    public void deleteUser(String uid, OnUserCallback callback) {
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(uid)) {
            callback.onError("Нельзя удалить свой аккаунт");
            return;
        }

        db.collection(USERS_COLLECTION).document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User deleted successfully: " + uid);
                    loadAllUsers();
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Ошибка удаления: " + e.getMessage()));
    }

    @Override
    public void resetPassword(String email, OnUserCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError("Ошибка отправки письма: " + e.getMessage()));
    }

    private User documentSnapshotToUser(DocumentSnapshot doc) {
        if (!doc.exists()) return null;

        User user = new User();
        user.setUid(doc.getString("uid"));
        user.setEmail(doc.getString("email"));
        user.setName(doc.getString("name"));

        // Исправление: если role == null, ставим "INSPECTOR"
        String role = doc.getString("role");
        user.setRole(role != null ? role : "INSPECTOR");

        user.setDepartment(doc.getString("department"));
        Boolean isActive = doc.getBoolean("isActive");
        user.setActive(isActive != null ? isActive : true);
        return user;
    }

    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "Пользователь";
        return email.split("@")[0];
    }

    @Override
    public LiveData<Resource<User>> login(String email, String password) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    db.collection(USERS_COLLECTION).document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    User user = documentSnapshotToUser(doc);
                                    currentUser.postValue(user);
                                    result.setValue(Resource.success(user));
                                } else {
                                    result.setValue(Resource.error("Пользователь не найден", null));
                                }
                            })
                            .addOnFailureListener(e -> result.setValue(Resource.error("Ошибка загрузки: " + e.getMessage(), null)));
                })
                .addOnFailureListener(e -> result.setValue(Resource.error("Неверный email или пароль", null)));

        return result;
    }

    @Override
    public void logout() {
        auth.signOut();
        currentUser.postValue(null);
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    @Override
    public String getAccessToken() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}