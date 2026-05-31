package com.example.e68.app.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentLoginBinding;
import com.example.e68.app.presentation.MainActivity;
import com.example.e68.app.presentation.common.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    private LoginViewModel viewModel;

    @Override
    protected FragmentLoginBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                  @Nullable ViewGroup container) {
        return FragmentLoginBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.loginButton.setOnClickListener(v -> {
            String email    = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            viewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(error);
        });

        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;

            MainActivity activity = (MainActivity) requireActivity();

            // Обновляем навигацию после входа
            activity.refreshNavigationAfterLogin();

            // Применяем меню для роли
            if (user.isInspector()) {
                activity.applyRoleMenu("INSPECTOR");
            } else if (user.isManager()) {
                activity.applyRoleMenu("MANAGER");
            } else if (user.isAdmin()) {
                activity.applyRoleMenu("ADMIN");
            }

            // Просто закрываем LoginFragment, возвращаясь назад
            // Главный экран уже будет показывать правильный startDestination
            NavController navController = Navigation.findNavController(requireView());

            // Проверяем текущий destination и действуем соответственно
            int currentId = navController.getCurrentDestination() != null
                    ? navController.getCurrentDestination().getId() : -1;

            if (currentId == R.id.loginFragment) {
                // Если мы всё ещё на LoginFragment, просто идём назад
                navController.popBackStack();
            } else {
                // Если нет, пробуем найти главный экран
                navController.navigate(R.id.nav_defects);
            }
        });
    }
}