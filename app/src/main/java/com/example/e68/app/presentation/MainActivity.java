package com.example.e68.app.presentation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.e68.app.R;
import com.example.e68.app.databinding.ActivityMainBinding;
import com.example.e68.app.data.repository.UserRepositoryImpl;
import com.example.e68.app.domain.entity.User;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Inject
    UserRepositoryImpl userRepository;  // Инжектим репозиторий пользователей

    private ActivityMainBinding binding;
    private NavController navController;

    // Храним роль текущего пользователя
    private String currentUserRole = "INSPECTOR"; // По умолчанию инспектор

    // Экраны где нижняя панель СКРЫТА
    private static final Set<Integer> HIDE_NAV = new HashSet<>(Arrays.asList(
            R.id.loginFragment,
            R.id.createDefectFragment,
            R.id.defectDetailFragment
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavController();
        observeCurrentUser();
    }

    /**
     * Наблюдаем за текущим пользователем, чтобы обновлять роль
     */
    private void observeCurrentUser() {
        userRepository.getCurrentUser().observe(this, user -> {
            if (user != null) {
                String role = getRoleString(user);
                if (!role.equals(currentUserRole)) {
                    currentUserRole = role;
                    applyRoleMenu(role);
                    setStartDestinationBasedOnRole();
                }
            }
        });
    }

    private void setupNavController() {
        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            Log.e(TAG, "nav_host_fragment not found!");
            return;
        }
        navController = host.getNavController();

        // Устанавливаем startDestination в зависимости от роли
        setStartDestinationBasedOnRole();

        // Показываем/скрываем нижнюю панель
        navController.addOnDestinationChangedListener((ctrl, dest, args) -> {
            boolean hide = HIDE_NAV.contains(dest.getId());
            binding.bottomNavigation.setVisibility(hide ? View.GONE : View.VISIBLE);
        });

        // Тапы по вкладкам — ВРУЧНУЮ без NavigationUI
        binding.bottomNavigation.setOnItemSelectedListener(item -> safeNavigateTo(item.getItemId()));

        // Повторный тап на активной вкладке — ничего не делать
        binding.bottomNavigation.setOnItemReselectedListener(item -> { /* no-op */ });
    }

    /**
     * Устанавливает startDestination в зависимости от роли
     */
    private void setStartDestinationBasedOnRole() {
        int startDestId = getProfileDestinationForRole(currentUserRole);
        Log.d(TAG, "Setting startDestination for role " + currentUserRole + " to: " + startDestId);
        setStartDestination(startDestId);
    }

    /**
     * Возвращает строку роли пользователя
     */
    private String getRoleString(User user) {
        if (user == null) return "INSPECTOR";
        String role = user.getRole();
        if (role == null) return "INSPECTOR";

        switch (role.toUpperCase()) {
            case "ADMIN":
                return "ADMIN";
            case "MANAGER":
                return "MANAGER";
            default:
                return "INSPECTOR";
        }
    }

    /**
     * Возвращает ID фрагмента профиля для конкретной роли
     * ПРОФИЛЬ - первый экран после входа!
     */
    private int getProfileDestinationForRole(String role) {
        if (role == null) return R.id.nav_profile;

        switch (role) {
            case "ADMIN":
                return R.id.nav_admin_profile;    // Профиль администратора
            case "MANAGER":
                return R.id.nav_manager_profile;  // Профиль менеджера
            default:
                return R.id.nav_profile;           // Профиль инспектора
        }
    }

    /**
     * Устанавливает новый startDestination в графе навигации
     */
    private void setStartDestination(int startDestId) {
        if (navController == null) return;

        NavGraph navGraph = navController.getGraph();
        int currentStartDest = navGraph.getStartDestination();

        if (currentStartDest == startDestId) {
            Log.d(TAG, "Start destination already set to " + startDestId);
            return;
        }

        navGraph.setStartDestination(startDestId);
        Log.d(TAG, "Start destination changed from " + currentStartDest + " to " + startDestId);
    }

    /**
     * Безопасный navigate(): не крашится, не дублирует back stack.
     */
    private boolean safeNavigateTo(int destId) {
        if (navController == null) return false;

        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destId) return true;

        try {
            NavOptions opts = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .build();
            navController.navigate(destId, null, opts);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unknown destination " + destId + ": " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Устанавливает нижнее меню по роли
     */
    public void applyRoleMenu(String role) {
        if (role == null) role = "INSPECTOR";
        currentUserRole = role; // Сохраняем роль
        int menuRes;
        switch (role.toUpperCase()) {
            case "MANAGER":
                menuRes = R.menu.menu_bottom_nav_manager;
                break;
            case "ADMIN":
                menuRes = R.menu.menu_bottom_nav_admin;
                break;
            default:
                menuRes = R.menu.menu_bottom_nav_inspector;
                break;
        }
        binding.bottomNavigation.getMenu().clear();
        binding.bottomNavigation.inflateMenu(menuRes);
        Log.d(TAG, "Menu applied for role: " + role);
    }

    /**
     * Обновляет навигацию после входа
     */
    public void refreshNavigationAfterLogin() {
        Log.d(TAG, "Refreshing navigation after login");
        setStartDestinationBasedOnRole();
    }

    public void showOfflineBanner(boolean show, int pendingCount) {
        if (binding.offlineBanner == null) return;
        binding.offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && pendingCount > 0) {
            binding.pendingCount.setText(pendingCount + " в очереди");
        }
    }
}