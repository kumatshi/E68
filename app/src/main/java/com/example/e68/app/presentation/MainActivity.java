// FILE: app/src/main/java/com/example/e68/app/presentation/MainActivity.java
// ЗАМЕНИ ПОЛНОСТЬЮ
package com.example.e68.app.presentation;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.e68.app.R;
import com.example.e68.app.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    private final List<Integer> noNavFragments = Arrays.asList(
            R.id.loginFragment,
            R.id.createDefectFragment,
            R.id.defectDetailFragment
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupNavigation();
        checkAutoLogin();
    }

    private void checkAutoLogin() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (navController == null) return;
                    if (navController.getCurrentDestination() == null) return;
                    if (navController.getCurrentDestination().getId() != R.id.loginFragment) return;
                    String role = (doc.exists() && doc.getString("role") != null)
                            ? doc.getString("role") : "INSPECTOR";
                    applyRoleMenu(role);
                    navigateByRole(role);
                })
                .addOnFailureListener(e -> {
                    applyRoleMenu("INSPECTOR");
                    navigateByRole("INSPECTOR");
                });
    }

    /** Вызывается из LoginFragment после успешного входа */
    public void applyRoleMenu(String role) {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.getMenu().clear();
        switch (role) {
            case "MANAGER":
                bottomNav.inflateMenu(R.menu.menu_bottom_nav_manager);
                break;
            case "ADMIN":
                bottomNav.inflateMenu(R.menu.menu_bottom_nav_admin);
                break;
            default:
                bottomNav.inflateMenu(R.menu.menu_bottom_nav_inspector);
                break;
        }
        // Переподключаем NavController к обновлённому меню
        NavigationUI.setupWithNavController(bottomNav, navController);
        bottomNav.setOnItemReselectedListener(item -> {});
    }

    private void navigateByRole(String role) {
        try {
            switch (role) {
                case "MANAGER":
                    navController.navigate(R.id.action_loginFragment_to_managerFragment);
                    break;
                case "ADMIN":
                    navController.navigate(R.id.action_loginFragment_to_adminFragment);
                    break;
                default:
                    navController.navigate(R.id.action_loginFragment_to_mainFragment);
                    break;
            }
        } catch (Exception ignored) {}
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = binding.bottomNavigation;
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideNav = noNavFragments.contains(destination.getId());
            binding.bottomNavigation.setVisibility(hideNav ? View.GONE : View.VISIBLE);
        });

        bottomNav.setOnItemReselectedListener(item -> {});
    }

    public void showOfflineBanner(boolean show, int pendingCount) {
        binding.offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && pendingCount > 0) {
            binding.pendingCount.setText(pendingCount + " в очереди");
        }
    }
}