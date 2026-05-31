package com.example.e68.app.presentation.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.e68.app.data.repository.AuthRepositoryImpl;
import com.example.e68.app.data.session.SessionManager;
import com.example.e68.app.domain.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";

    private final AuthRepositoryImpl authRepository;
    private final SessionManager sessionManager;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public LiveData<User> getUser() { return _user; }

    private final MutableLiveData<Boolean> _logoutDone = new MutableLiveData<>();
    public LiveData<Boolean> getLogoutDone() { return _logoutDone; }

    // Инспектор - статистика дефектов
    private final MutableLiveData<Integer> _defectsCreated = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _defectsResolved = new MutableLiveData<>(0);

    // Менеджер - статистика отчётов
    private final MutableLiveData<Integer> _reportsGenerated = new MutableLiveData<>(0);

    // Администратор - статистика пользователей
    private final MutableLiveData<Integer> _usersAdded = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _usersEdited = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _usersDeleted = new MutableLiveData<>(0);

    public LiveData<Integer> getDefectsCreated() { return _defectsCreated; }
    public LiveData<Integer> getDefectsResolved() { return _defectsResolved; }
    public LiveData<Integer> getReportsGenerated() { return _reportsGenerated; }
    public LiveData<Integer> getUsersAdded() { return _usersAdded; }
    public LiveData<Integer> getUsersEdited() { return _usersEdited; }
    public LiveData<Integer> getUsersDeleted() { return _usersDeleted; }

    @Inject
    public ProfileViewModel(AuthRepositoryImpl authRepository, SessionManager sessionManager) {
        this.authRepository = authRepository;
        this.sessionManager = sessionManager;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();

        _user.setValue(null);
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        com.google.firebase.auth.FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            android.util.Log.e(TAG, "No Firebase user found");
            return;
        }

        String uid = firebaseUser.getUid();
        android.util.Log.d(TAG, "loadCurrentUser - UID: " + uid);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = new User();
                        user.setUid(uid);
                        user.setName(getStr(doc, "name"));
                        user.setEmail(getStr(doc, "email"));
                        user.setRole(getStr(doc, "role"));
                        user.setDepartment(getStr(doc, "department"));
                        Object activeVal = doc.getData().get("isActive");
                        if (activeVal == null) activeVal = doc.getData().get("active");
                        user.setActive(Boolean.TRUE.equals(activeVal));
                        _user.postValue(user);

                        String role = user.getRole();
                        android.util.Log.d(TAG, "User role: " + role);

                        if (role != null) {
                            switch (role) {
                                case "INSPECTOR":
                                    android.util.Log.d(TAG, "Loading INSPECTOR stats for uid: " + uid);
                                    loadInspectorStats(uid);
                                    break;
                                case "MANAGER":
                                    android.util.Log.d(TAG, "Loading MANAGER stats");
                                    loadManagerStats(uid);
                                    break;
                                case "ADMIN":
                                    android.util.Log.d(TAG, "Loading ADMIN stats");
                                    loadAdminStats(uid);
                                    break;
                                default:
                                    android.util.Log.d(TAG, "Unknown role: " + role);
                            }
                        }
                    } else {
                        android.util.Log.e(TAG, "User document not found for uid: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Failed to load user: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────
    // ИНСПЕКТОР - статистика дефектов
    // ─────────────────────────────────────────────────────────────
    private void loadInspectorStats(String uid) {
        android.util.Log.d(TAG, "=== loadInspectorStats START ===");
        android.util.Log.d(TAG, "Searching defects with inspectorUid: " + uid);

        db.collection("defects")
                .whereEqualTo("inspectorUid", uid)
                .get()
                .addOnSuccessListener(query -> {
                    android.util.Log.d(TAG, "SUCCESS: Found " + query.size() + " defects by inspectorUid");

                    int total = query.size();
                    _defectsCreated.postValue(total);

                    int resolved = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        String status = doc.getString("status");
                        String title = doc.getString("title");
                        android.util.Log.d(TAG, "Defect: " + title + ", status: " + status);
                        if ("RESOLVED".equals(status)) {
                            resolved++;
                        }
                    }
                    android.util.Log.d(TAG, "Total: " + total + ", Resolved: " + resolved);
                    _defectsResolved.postValue(resolved);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "FAILED: Error by inspectorUid: " + e.getMessage());
                    _defectsCreated.postValue(0);
                    _defectsResolved.postValue(0);
                });
    }

    // ─────────────────────────────────────────────────────────────
    // МЕНЕДЖЕР - статистика отчётов
    // ─────────────────────────────────────────────────────────────
    private void loadManagerStats(String uid) {
        android.util.Log.d(TAG, "=== loadManagerStats START ===");

        db.collection("defects")
                .whereEqualTo("status", "RESOLVED")
                .get()
                .addOnSuccessListener(query -> {
                    android.util.Log.d(TAG, "Found " + query.size() + " resolved defects");
                    _reportsGenerated.postValue(query.size());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Error loading manager stats: " + e.getMessage());
                    _reportsGenerated.postValue(0);
                });
    }

    // ─────────────────────────────────────────────────────────────
    // АДМИНИСТРАТОР - статистика пользователей
    // ─────────────────────────────────────────────────────────────
    private void loadAdminStats(String uid) {
        android.util.Log.d(TAG, "=== loadAdminStats START ===");

        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    android.util.Log.d(TAG, "Found " + query.size() + " users");
                    int total = query.size();
                    _usersAdded.postValue(total);

                    int active = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        Boolean isActive = doc.getBoolean("isActive");
                        if (isActive != null && isActive) {
                            active++;
                        }
                    }
                    _usersEdited.postValue(active);
                    _usersDeleted.postValue(total - active);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Error loading admin stats: " + e.getMessage());
                    _usersAdded.postValue(0);
                    _usersEdited.postValue(0);
                    _usersDeleted.postValue(0);
                });
    }

    public void logout() {
        authRepository.logout();
        sessionManager.clearSession();
        _logoutDone.postValue(true);
    }

    private String getStr(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        String v = doc.getString(key);
        return v != null ? v : "";
    }
}