package com.example.e68.app.domain.repository;

import androidx.lifecycle.LiveData;

import com.example.e68.app.domain.entity.User;
import com.example.e68.app.util.Resource;
import com.example.e68.app.util.Resource;

import java.util.List;

public interface UserRepository {

    // Текущий пользователь
    LiveData<User> getCurrentUser();

    // Все пользователи (для админа)
    LiveData<List<User>> getAllUsers();

    // CRUD операции
    void createUser(String email, String password, String name, String role, String department, OnUserCallback callback);
    void updateUser(String uid, String name, String role, String department, boolean isActive, OnUserCallback callback);
    void deleteUser(String uid, OnUserCallback callback);
    void resetPassword(String email, OnUserCallback callback);

    // Аутентификация
    LiveData<Resource<User>> login(String email, String password);
    void logout();
    boolean isLoggedIn();
    String getAccessToken();

    interface OnUserCallback {
        void onSuccess();
        void onError(String message);
    }
}