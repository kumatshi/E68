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
import com.example.e68.app.data.session.SessionManager;
import com.example.e68.app.domain.entity.User;
import com.example.e68.app.presentation.common.BaseFragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import androidx.navigation.NavController;

@AndroidEntryPoint
public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    private ProfileViewModel viewModel;

    @Inject
    SessionManager sessionManager;

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
        setupStatsVisibility();
    }

    private void setupStatsVisibility() {
        String userRole = sessionManager != null ? sessionManager.getUserRole() : "INSPECTOR";

        // Скрываем все блоки статистики по умолчанию
        binding.statsInspectorLayout.setVisibility(View.GONE);
        binding.statsManagerLayout.setVisibility(View.GONE);
        binding.statsAdminLayout.setVisibility(View.GONE);

        // Показываем нужный блок в зависимости от роли
        switch (userRole) {
            case "INSPECTOR":
                binding.statsInspectorLayout.setVisibility(View.VISIBLE);
                binding.statsTitle.setText("Моя статистика");
                break;
            case "MANAGER":
                binding.statsManagerLayout.setVisibility(View.VISIBLE);
                binding.statsTitle.setText("Статистика отчётов");
                break;
            case "ADMIN":
                binding.statsAdminLayout.setVisibility(View.VISIBLE);
                binding.statsTitle.setText("Статистика действий");
                break;
        }
    }

    private void observeViewModel() {
        viewModel.getUser().observe(getViewLifecycleOwner(), this::bindUser);

        // Инспектор - статистика дефектов
        viewModel.getDefectsCreated().observe(getViewLifecycleOwner(),
                n -> binding.tvDefectsCreated.setText(String.valueOf(n)));
        viewModel.getDefectsResolved().observe(getViewLifecycleOwner(),
                n -> binding.tvDefectsResolved.setText(String.valueOf(n)));

        // Менеджер - статистика отчётов
        viewModel.getReportsGenerated().observe(getViewLifecycleOwner(),
                n -> binding.tvReportsGenerated.setText(String.valueOf(n)));

        // Администратор - статистика пользователей
        viewModel.getUsersAdded().observe(getViewLifecycleOwner(),
                n -> binding.tvUsersAdded.setText(String.valueOf(n)));
        viewModel.getUsersEdited().observe(getViewLifecycleOwner(),
                n -> binding.tvUsersEdited.setText(String.valueOf(n)));
        viewModel.getUsersDeleted().observe(getViewLifecycleOwner(),
                n -> binding.tvUsersDeleted.setText(String.valueOf(n)));

        viewModel.getLogoutDone().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        try {
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_global_logout);
        } catch (IllegalArgumentException e) {
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.loginFragment);
            navController.popBackStack(R.id.nav_graph, true);
        }
    }

    private void bindUser(User user) {
        if (user == null) return;

        String initials = buildInitials(user.getName());
        binding.tvAvatarInitials.setText(initials);

        binding.tvProfileName.setText(
                user.getName() != null && !user.getName().isEmpty()
                        ? user.getName() : user.getEmail());

        binding.tvProfileEmail.setText(user.getEmail() != null ? user.getEmail() : "");

        String roleLabel = roleToLabel(user.getRole());
        binding.tvProfileRole.setText(roleLabel);
        binding.tvProfileRole.setBackgroundResource(roleBadgeColor(user.getRole()));

        String dept = user.getDepartment();
        binding.tvDepartment.setText(
                dept != null && !dept.isEmpty() ? dept : "Не назначен");

        if (user.isActive()) {
            binding.tvAccountStatus.setText("Активен");
            binding.tvAccountStatus.setTextColor(
                    requireContext().getColor(R.color.status_resolved));
        } else {
            binding.tvAccountStatus.setText("Заблокирован");
            binding.tvAccountStatus.setTextColor(
                    requireContext().getColor(R.color.status_open));
        }

        String uid = user.getUid() != null ? user.getUid() : "";
        binding.tvUserId.setText(uid.length() > 20 ? uid.substring(0, 20) + "…" : uid);
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());
        binding.rowAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выйти из аккаунта?")
                .setMessage("Вы уверены, что хотите выйти?")
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
                .setTitle("E68")
                .setMessage("Версия: " + version
                        + "\n\nПриложение разработал студент группы 4ПК2 Тазеев М.Э."
                        + "\n\n2026")
                .setPositiveButton("OK", null)
                .show();
    }
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
        return R.drawable.bg_role_badge;
    }
}