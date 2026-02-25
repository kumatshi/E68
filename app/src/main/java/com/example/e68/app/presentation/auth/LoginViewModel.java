package com.example.e68.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.domain.repository.UserRepository;
import com.example.e68.app.presentation.common.BaseViewModel;
import com.example.e68.app.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> _loginSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoginSuccess() { return _loginSuccess; }

    @Inject
    public LoginViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        setLoading(true);

        userRepository.login(email, password).observeForever(resource -> {
            setLoading(false);

            if (resource.status == Resource.Status.SUCCESS) {
                _loginSuccess.postValue(true);
            } else if (resource.status == Resource.Status.ERROR) {
                showError(resource.message);
            }
        });
    }
}