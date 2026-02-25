package com.example.e68.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.util.Resource;

public interface UserRepository {

    LiveData<User> getCurrentUser();

    LiveData<Resource<User>> login(String email, String password);

    void logout();

    boolean isLoggedIn();

    String getAccessToken();
}