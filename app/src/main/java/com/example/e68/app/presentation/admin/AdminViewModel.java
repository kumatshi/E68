package com.example.e68.app.presentation.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.e68.app.data.repository.UserRepositoryImpl;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.domain.repository.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final DefectRepository defectRepository;

    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<User> selectedUser = new MutableLiveData<>();

    @Inject
    public AdminViewModel(UserRepository userRepository, DefectRepository defectRepository) {
        this.userRepository = userRepository;
        this.defectRepository = defectRepository;
        loadUsers();
    }

    // =========================================================
    // ПОЛЬЗОВАТЕЛИ
    // =========================================================

    public LiveData<List<User>> getUsers() {
        return userRepository.getAllUsers();
    }

    public void loadUsers() {
        isLoading.setValue(true);
        // Данные загружаются через LiveData в репозитории
        isLoading.setValue(false);
    }

    public void createUser(String email, String password, String name, String role, String department) {
        isLoading.setValue(true);
        userRepository.createUser(email, password, name, role, department, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Пользователь успешно создан");
                loadUsers();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void updateUser(String uid, String name, String role, String department, boolean isActive) {
        isLoading.setValue(true);
        userRepository.updateUser(uid, name, role, department, isActive, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Пользователь обновлён");
                loadUsers();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void deleteUser(User user) {
        if (user == null) return;

        isLoading.setValue(true);
        userRepository.deleteUser(user.getUid(), new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Пользователь удалён");
                loadUsers();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void resetPassword(String email) {
        isLoading.setValue(true);
        userRepository.resetPassword(email, new UserRepository.OnUserCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                successMessage.setValue("Письмо для сброса пароля отправлено на " + email);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    public void selectUser(User user) {
        selectedUser.setValue(user);
    }

    public LiveData<User> getSelectedUser() {
        return selectedUser;
    }

    // =========================================================
    // СТАТИСТИКА И ОТЧЁТЫ
    // =========================================================

    public LiveData<Integer> getTotalUsersCount() {
        MutableLiveData<Integer> count = new MutableLiveData<>(0);
        userRepository.getAllUsers().observeForever(users -> {
            if (users != null) {
                count.setValue(users.size());
            }
        });
        return count;
    }

    public LiveData<Integer> getActiveUsersCount() {
        MutableLiveData<Integer> count = new MutableLiveData<>(0);
        userRepository.getAllUsers().observeForever(users -> {
            if (users != null) {
                int active = 0;
                for (User user : users) {
                    if (user.isActive()) active++;
                }
                count.setValue(active);
            }
        });
        return count;
    }

    public LiveData<Integer> getUsersByRole(String role) {
        MutableLiveData<Integer> count = new MutableLiveData<>(0);
        userRepository.getAllUsers().observeForever(users -> {
            if (users != null) {
                int roleCount = 0;
                for (User user : users) {
                    if (role.equals(user.getRole())) roleCount++;
                }
                count.setValue(roleCount);
            }
        });
        return count;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }
}