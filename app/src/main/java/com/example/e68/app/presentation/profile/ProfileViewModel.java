// FILE: app/src/main/java/com/example/e68/app/presentation/profile/ProfileViewModel.java
package com.example.e68.app.presentation.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.e68.app.data.repository.AuthRepositoryImpl;
import com.example.e68.app.domain.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final AuthRepositoryImpl authRepository;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public LiveData<User> getUser() { return _user; }

    private final MutableLiveData<Boolean> _logoutDone = new MutableLiveData<>();
    public LiveData<Boolean> getLogoutDone() { return _logoutDone; }

    // Статистика
    private final MutableLiveData<Integer> _defectsCreated  = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _defectsResolved = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> _patrolsCount    = new MutableLiveData<>(0);

    public LiveData<Integer> getDefectsCreated()  { return _defectsCreated; }
    public LiveData<Integer> getDefectsResolved() { return _defectsResolved; }
    public LiveData<Integer> getPatrolsCount()    { return _patrolsCount; }

    @Inject
    public ProfileViewModel(AuthRepositoryImpl authRepository) {
        this.authRepository = authRepository;
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();

        // Подписываемся на текущего пользователя из репозитория
        _user.setValue(null);
        loadCurrentUser();
    }

    private void loadCurrentUser() {
        com.google.firebase.auth.FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();

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

                        // Загружаем статистику
                        loadStats(uid, user.getRole());
                    }
                });
    }

    private void loadStats(String uid, String role) {
        // Дефекты созданные этим пользователем
        db.collection("defects")
                .whereEqualTo("createdByUid", uid)
                .get()
                .addOnSuccessListener(q -> {
                    _defectsCreated.postValue(q.size());

                    long resolved = q.getDocuments().stream()
                            .filter(d -> "RESOLVED".equals(d.getString("status")))
                            .count();
                    _defectsResolved.postValue((int) resolved);
                })
                .addOnFailureListener(e -> {
                    // Firestore коллекция ещё пуста — оставляем 0
                });

        // Объезды
        db.collection("routes")
                .whereEqualTo("inspectorUid", uid)
                .get()
                .addOnSuccessListener(q -> _patrolsCount.postValue(q.size()))
                .addOnFailureListener(e -> { /* коллекция пуста */ });
    }

    public void logout() {
        authRepository.logout();
        _logoutDone.postValue(true);
    }

    private String getStr(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        String v = doc.getString(key);
        return v != null ? v : "";
    }
}