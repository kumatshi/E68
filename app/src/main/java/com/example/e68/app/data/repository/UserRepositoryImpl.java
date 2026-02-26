package com.example.e68.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.domain.repository.UserRepository;
import com.example.e68.app.util.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserRepositoryImpl implements UserRepository {

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();

    @Inject
    public UserRepositoryImpl() {
        // Инициализация
    }

    @Override
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    @Override
    public LiveData<Resource<User>> login(String email, String password) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        // Имитация логина
        new android.os.Handler().postDelayed(() -> {
            if (email.equals("test@test.com") && password.equals("123456")) {
                User user = new User();

                user.setEmail(email);
                user.setName("Тестовый пользователь");
                user.setRole("INSPECTOR");

                currentUser.postValue(user);
                result.postValue(Resource.success(user));
            } else {
                result.postValue(Resource.error("Неверный email или пароль", null));
            }
        }, 1000);

        return result;
    }

    @Override
    public void logout() {
        currentUser.postValue(null);
    }

    @Override
    public boolean isLoggedIn() {
        return currentUser.getValue() != null;
    }

    @Override
    public String getAccessToken() {
        return "test_token";
    }
}