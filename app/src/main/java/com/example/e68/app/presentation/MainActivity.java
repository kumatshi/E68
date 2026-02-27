package com.example.e68.app.presentation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.e68.app.R;
import com.example.e68.app.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MainActivity — исправленная версия.
 *
 * ╔══ ПРОБЛЕМА ══════════════════════════════════════════════════╗
 * ║ NavigationUI.setupWithNavController() привязывает bottomNav  ║
 * ║ к стартовому графу (инспектор). Когда менеджер/админ         ║
 * ║ нажимает свои вкладки — NavController не находит destination  ║
 * ║ → IllegalArgumentException → краш.                           ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * ╔══ РЕШЕНИЕ ═══════════════════════════════════════════════════╗
 * ║ 1. Убран NavigationUI.setupWithNavController().              ║
 * ║ 2. Ручной setOnItemSelectedListener + try/catch.             ║
 * ║ 3. Проверка "уже на этом экране" перед navigate().           ║
 * ║ 4. applyRoleMenu() меняет меню ДО первого тапа по вкладке.  ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private NavController navController;

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
    }

    private void setupNavController() {
        NavHostFragment host = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            Log.e(TAG, "nav_host_fragment not found!");
            return;
        }
        navController = host.getNavController();

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
     * Безопасный navigate(): не крашится, не дублирует back stack.
     */
    private boolean safeNavigateTo(int destId) {
        if (navController == null) return false;

        NavDestination current = navController.getCurrentDestination();
        // Уже на этом экране
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
     * Устанавливает нижнее меню по роли.
     * Вызывать из LoginFragment ДО navigate():
     *
     *   ((MainActivity) requireActivity()).applyRoleMenu("MANAGER");
     *   Navigation.findNavController(requireView())
     *       .navigate(R.id.action_loginFragment_to_managerFragment);
     */
    public void applyRoleMenu(String role) {
        if (role == null) role = "INSPECTOR";
        int menuRes;
        switch (role.toUpperCase()) {
            case "MANAGER": menuRes = R.menu.menu_bottom_nav_manager; break;
            case "ADMIN":   menuRes = R.menu.menu_bottom_nav_admin;   break;
            default:        menuRes = R.menu.menu_bottom_nav_inspector; break;
        }
        binding.bottomNavigation.getMenu().clear();
        binding.bottomNavigation.inflateMenu(menuRes);
        Log.d(TAG, "Menu applied for role: " + role);
    }

    public void showOfflineBanner(boolean show, int pendingCount) {
        if (binding.offlineBanner == null) return;
        binding.offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && pendingCount > 0) {
            binding.pendingCount.setText(pendingCount + " в очереди");
        }
    }
}