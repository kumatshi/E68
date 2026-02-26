package com.example.e68.app.presentation;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import dagger.hilt.android.AndroidEntryPoint;
import com.example.e68.app.R;
import com.example.e68.app.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    // Fragments that should HIDE the bottom nav
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
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        // Wire bottom nav to nav controller
        BottomNavigationView bottomNav = binding.bottomNavigation;
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Show/hide bottom nav based on destination
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideNav = noNavFragments.contains(destination.getId());
            binding.bottomNavigation.setVisibility(hideNav ? View.GONE : View.VISIBLE);
        });

        // Handle bottom nav item reselection (scroll to top)
        bottomNav.setOnItemReselectedListener(item -> {
            // No-op prevents re-navigating to same dest
        });
    }

    public void showOfflineBanner(boolean show, int pendingCount) {
        binding.offlineBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && pendingCount > 0) {
            binding.pendingCount.setText(pendingCount + " в очереди");
        }
    }
}
