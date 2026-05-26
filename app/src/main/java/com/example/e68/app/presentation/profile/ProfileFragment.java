package com.example.e68.app.presentation.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentProfileBinding;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.presentation.common.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;
import androidx.navigation.NavController;

@AndroidEntryPoint
public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel viewModel;

    @Override
    protected FragmentProfileBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                    @Nullable ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();
        setupListeners();
    }

    private void observeViewModel() {
        viewModel.getUser().observe(getViewLifecycleOwner(), this::bindUser);
        viewModel.getDefectsCreated().observe(getViewLifecycleOwner(),
                n -> binding.tvStatCreated.setText(String.valueOf(n)));
        viewModel.getDefectsResolved().observe(getViewLifecycleOwner(),
                n -> binding.tvStatResolved.setText(String.valueOf(n)));
        viewModel.getPatrolsCount().observe(getViewLifecycleOwner(),
                n -> binding.tvStatPatrols.setText(String.valueOf(n)));

        // ИСПРАВЛЕНО: Используем глобальное действие для выхода
        viewModel.getLogoutDone().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                navigateToLogin();
            }
        });
    }

    /**
     * Навигация на экран входа с использованием глобального действия.
     * Это гарантирует, что весь стек навигации будет очищен.
     */
    private void navigateToLogin() {
        try {
            // Используем глобальное действие для выхода
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_global_logout);
        } catch (IllegalArgumentException e) {
            // Fallback на случай, если глобальное действие недоступно
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.loginFragment);
            navController.popBackStack(R.id.nav_graph, true);
        }
    }

    private void bindUser(User user) {
        if (user == null) return;

        // Инициалы
        String initials = buildInitials(user.getName());
        binding.tvAvatarInitials.setText(initials);

        // Имя
        binding.tvProfileName.setText(
                user.getName() != null && !user.getName().isEmpty()
                        ? user.getName() : user.getEmail());

        // Email
        binding.tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        // Роль — текст и цвет
        String roleLabel = roleToLabel(user.getRole());
        binding.tvProfileRole.setText(roleLabel);
        binding.tvProfileRole.setBackgroundResource(roleBadgeColor(user.getRole()));

        // Подразделение
        String dept = user.getDepartment();
        binding.tvDepartment.setText(
                dept != null && !dept.isEmpty() ? dept : "Не назначен");

        // Статус аккаунта
        if (user.isActive()) {
            binding.tvAccountStatus.setText("Активен");
            binding.tvAccountStatus.setTextColor(
                    requireContext().getColor(R.color.status_resolved));
        } else {
            binding.tvAccountStatus.setText("Заблокирован");
            binding.tvAccountStatus.setTextColor(
                    requireContext().getColor(R.color.status_open));
        }

        // UID (обрезаем для читаемости)
        String uid = user.getUid() != null ? user.getUid() : "";
        binding.tvUserId.setText(uid.length() > 20 ? uid.substring(0, 20) + "…" : uid);
    }

    private void setupListeners() {
        // Кнопка выйти — диалог подтверждения
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        // О приложении
        binding.rowAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выйти из аккаунта?")
                .setMessage("Вы уверены, что хотите выйти? Все несинхронизированные данные будут сохранены.")
                .setPositiveButton("Выйти", (dialog, which) -> viewModel.logout())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAboutDialog() {
        String version = "1.0.0";
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception ignored) {}

        new AlertDialog.Builder(requireContext())
                .setTitle("RoadPatrol")
                .setMessage("Версия: " + version
                        + "\n\nСистема мониторинга дорожных дефектов\nпо ГОСТ Р 50597-2017"
                        + "\n\n© 2025 Все права защищены")
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Хелперы ──────────────────────────────────────────────────────

    private String buildInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    private String roleToLabel(String role) {
        if (role == null) return "ПОЛЬЗОВАТЕЛЬ";
        switch (role) {
            case "INSPECTOR": return "ИНСПЕКТОР";
            case "MANAGER":   return "МЕНЕДЖЕР";
            case "ADMIN":     return "АДМИНИСТРАТОР";
            default:          return role;
        }
    }

    private int roleBadgeColor(String role) {
        if ("ADMIN".equals(role))    return R.drawable.bg_role_badge_admin;
        if ("MANAGER".equals(role)) return R.drawable.bg_role_badge_manager;
        return R.drawable.bg_role_badge; // INSPECTOR — оранжевый
    }
}