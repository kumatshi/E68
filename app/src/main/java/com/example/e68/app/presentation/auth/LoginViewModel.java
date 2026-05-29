package com.example.e68.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.data.repository.AuthRepositoryImpl;
import com.example.e68.app.data.session.SessionManager;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends BaseViewModel {

    private final AuthRepositoryImpl authRepository;
    private final SessionManager sessionManager;

    private final MutableLiveData<User> _loginSuccess = new MutableLiveData<>();
    public LiveData<User> getLoginSuccess() { return _loginSuccess; }

    @Inject
    public LoginViewModel(AuthRepositoryImpl authRepository, SessionManager sessionManager) {
        this.authRepository = authRepository;
        this.sessionManager = sessionManager;
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

                // Критически важно: проверяем что user не null и у него есть uid
                if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                    String role = user.getRole();
                    if (role == null || role.isEmpty()) {
                        role = "INSPECTOR";
                        user.setRole(role);
                    }
                    // Сохраняем сессию
                    sessionManager.saveLoginSession(user.getUid(), role);
                    _loginSuccess.postValue(user);
                } else {
                    showError("Ошибка получения данных пользователя");
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }
}