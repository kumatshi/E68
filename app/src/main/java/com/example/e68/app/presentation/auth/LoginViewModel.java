package com.example.e68.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.data.repository.AuthRepositoryImpl;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private final AuthRepositoryImpl authRepository;

    private final MutableLiveData<User> _loginSuccess = new MutableLiveData<>();
    public LiveData<User> getLoginSuccess() { return _loginSuccess; }

    @Inject
    public LoginViewModel(AuthRepositoryImpl authRepository) {
        this.authRepository = authRepository;
    }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля");
            return;
        }
        setLoading(true);
        authRepository.login(email, password, new AuthRepositoryImpl.OnLoginCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                _loginSuccess.postValue(user);
            }
            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }
}