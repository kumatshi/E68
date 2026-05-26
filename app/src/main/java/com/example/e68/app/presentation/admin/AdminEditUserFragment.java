package com.example.e68.app.presentation.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import android.widget.Spinner;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentAdminEditUserBinding;
import com.example.e68.app.domain.entity.User;
import android.widget.Spinner;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminEditUserFragment extends Fragment {

    private FragmentAdminEditUserBinding binding;
    private AdminViewModel viewModel;
    private String uid;
    private boolean isEditMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminEditUserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Получаем аргументы
        if (getArguments() != null) {
            uid = getArguments().getString("uid");
            if (uid != null && !uid.isEmpty()) {
                isEditMode = true;
                loadUserData();
            }
        }

        setupSpinners();
        observeViewModel();
        setupListeners();

        if (!isEditMode) {
            binding.tvTitle.setText("Добавление пользователя");
            binding.btnDelete.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        binding.tvTitle.setText("Редактирование пользователя");

        binding.etEmail.setText(getArguments().getString("email", ""));
        binding.etEmail.setEnabled(false);
        binding.etName.setText(getArguments().getString("name", ""));
        binding.etDepartment.setText(getArguments().getString("department", ""));

        String role = getArguments().getString("role", "INSPECTOR");
        setSpinnerSelection(binding.spinnerRole, role);

        boolean isActive = getArguments().getBoolean("isActive", true);
        binding.switchActive.setChecked(isActive);

        // Показываем поле пароля только при создании
        binding.layoutPassword.setVisibility(View.GONE);
    }

    private void setupSpinners() {
        // Роли
        String[] roles = {"INSPECTOR", "MANAGER", "ADMIN"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, roles);
        binding.spinnerRole.setAdapter(roleAdapter);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (spinner.getAdapter().getItem(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSave.setEnabled(!isLoading);
            binding.btnDelete.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> saveUser());
        binding.btnDelete.setOnClickListener(v -> deleteUser());
        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void saveUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String name = binding.etName.getText().toString().trim();
        String role = binding.spinnerRole.getSelectedItem().toString();
        String department = binding.etDepartment.getText().toString().trim();
        boolean isActive = binding.switchActive.isChecked();

        if (email.isEmpty()) {
            binding.etEmail.setError("Введите email");
            return;
        }

        if (!isEditMode && password.isEmpty()) {
            binding.etPassword.setError("Введите пароль");
            return;
        }

        if (isEditMode) {
            viewModel.updateUser(uid, name.isEmpty() ? null : name, role, department, isActive);
        } else {
            viewModel.createUser(email, password, name, role, department);
        }
    }

    private void deleteUser() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удаление пользователя")
                .setMessage("Вы уверены, что хотите удалить этого пользователя?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    User user = new User();
                    user.setUid(uid);
                    viewModel.deleteUser(user);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Email не указан", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Сброс пароля")
                .setMessage("Отправить письмо для сброса пароля на " + email + "?")
                .setPositiveButton("Отправить", (dialog, which) -> {
                    viewModel.resetPassword(email);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}