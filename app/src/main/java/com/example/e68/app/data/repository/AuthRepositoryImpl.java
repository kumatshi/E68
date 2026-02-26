package com.example.e68.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepositoryImpl {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    @Inject
    public AuthRepositoryImpl() {
        this.auth = FirebaseAuth.getInstance();
        this.db   = FirebaseFirestore.getInstance();
    }

    public LiveData<User> getCurrentUser() { return currentUser; }

    public void login(String email, String password,
                      OnLoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    // Загружаем роль из Firestore
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    User user = doc.toObject(User.class);
                                    if (user != null) {
                                        user.setUid(uid);
                                        currentUser.postValue(user);
                                        callback.onSuccess(user);
                                    } else {
                                        callback.onError("Пользователь не найден в базе");
                                    }
                                } else {
                                    callback.onError("Данные пользователя не найдены");
                                }
                            })
                            .addOnFailureListener(e ->
                                    callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onError("Неверный email или пароль"));
    }

    public void logout() {
        auth.signOut();
        currentUser.postValue(null);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public interface OnLoginCallback {
        void onSuccess(User user);
        void onError(String message);
    }
}