package com.example.e68.app.presentation.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentAdminUsersBinding;
import com.example.e68.app.domain.entity.User;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminUsersFragment extends Fragment {

    private FragmentAdminUsersBinding binding;
    private AdminViewModel viewModel;
    private AdminUsersAdapter adapter;
    private static final String TAG = "AdminUsersFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated: Фрагмент загружен!");  // ← Добавьте

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        observeViewModel();
        setupListeners();
    }

    private void setupRecyclerView() {
        adapter = new AdminUsersAdapter(user -> {
            // При клике на пользователя открываем редактирование
            Bundle args = new Bundle();
            args.putString("uid", user.getUid());
            args.putString("email", user.getEmail());
            args.putString("name", user.getName());
            args.putString("role", user.getRole());
            args.putString("department", user.getDepartment());
            args.putBoolean("isActive", user.isActive());

            Navigation.findNavController(requireView())
                    .navigate(R.id.action_adminUsersFragment_to_adminEditUserFragment, args);
        });

        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.usersRecyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                adapter.submitList(users);
                binding.countAllUsers.setText(String.valueOf(users.size()));

                // Подсчёт активных и заблокированных
                long activeCount = users.stream().filter(User::isActive).count();
                long blockedCount = users.size() - activeCount;
                binding.countActiveUsers.setText(String.valueOf(activeCount));
                binding.countBlockedUsers.setText(String.valueOf(blockedCount));
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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
                viewModel.clearMessages();
            }
        });
    }

    private void setupListeners() {
        // Кнопка "Добавить" в заголовке
        binding.btnAddUser.setOnClickListener(v -> {
            Log.d("AdminUsers", "btnAddUser clicked!");  // ← Добавьте этот лог
            try {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_adminUsersFragment_to_adminEditUserFragment);
            } catch (Exception e) {
                Log.e("AdminUsers", "Navigation error: " + e.getMessage());
                Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // FAB кнопка
        binding.fabAddUser.setOnClickListener(v -> {
            Log.d("AdminUsers", "fabAddUser clicked!");  // ← Добавьте этот лог
            try {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_adminUsersFragment_to_adminEditUserFragment);
            } catch (Exception e) {
                Log.e("AdminUsers", "Navigation error: " + e.getMessage());
                Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}