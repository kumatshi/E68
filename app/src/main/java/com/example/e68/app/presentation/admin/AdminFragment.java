// FILE: app/src/main/java/com/example/e68/app/presentation/admin/AdminFragment.java
// ЗАМЕНИ ПОЛНОСТЬЮ
package com.example.e68.app.presentation.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.e68.app.databinding.FragmentAdminUsersBinding;
import com.example.e68.app.domain.entity.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Стартовый экран администратора — список сотрудников.
 * ID = adminFragment → совпадает с nav_admin_users в меню.
 */
@AndroidEntryPoint
public class AdminFragment extends Fragment {

    private FragmentAdminUsersBinding binding;
    private final List<User> allUsers = new ArrayList<>();
    private UsersAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecycler();
        setupFilters();
        loadUsers();
        binding.btnAddUser.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Функция добавления в разработке", Toast.LENGTH_SHORT).show());
    }

    private void setupRecycler() {
        adapter = new UsersAdapter(new ArrayList<>(), user -> showUserOptions(user));
        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.usersRecyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.roleChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == binding.chipAllRoles.getId())        showFiltered("ALL");
            else if (id == binding.chipInspectors.getId()) showFiltered("INSPECTOR");
            else if (id == binding.chipManagers.getId())   showFiltered("MANAGER");
            else if (id == binding.chipActiveUsers.getId()) showFilteredActive();
        });
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance().collection("users").get()
                .addOnSuccessListener(query -> {
                    allUsers.clear();
                    int active = 0, blocked = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        User u = new User();
                        u.setUid(doc.getId());
                        u.setName(doc.getString("name") != null ? doc.getString("name") : "");
                        u.setEmail(doc.getString("email") != null ? doc.getString("email") : "");
                        u.setRole(doc.getString("role") != null ? doc.getString("role") : "INSPECTOR");
                        u.setDepartment(doc.getString("department") != null ? doc.getString("department") : "");
                        Object av = doc.getData().get("isActive");
                        if (av == null) av = doc.getData().get("active");
                        u.setActive(Boolean.TRUE.equals(av));
                        allUsers.add(u);
                        if (u.isActive()) active++; else blocked++;
                    }
                    binding.countAllUsers.setText(String.valueOf(allUsers.size()));
                    binding.countActiveUsers.setText(String.valueOf(active));
                    binding.countBlockedUsers.setText(String.valueOf(blocked));
                    adapter.setUsers(new ArrayList<>(allUsers));
                    binding.emptyState.setVisibility(allUsers.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showFiltered(String roleFilter) {
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            if ("ALL".equals(roleFilter) || roleFilter.equals(u.getRole())) filtered.add(u);
        }
        adapter.setUsers(filtered);
    }

    private void showFilteredActive() {
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) { if (u.isActive()) filtered.add(u); }
        adapter.setUsers(filtered);
    }

    private void showUserOptions(User user) {
        String[] options = {"Изменить роль", user.isActive() ? "Заблокировать" : "Разблокировать"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(user.getName().isEmpty() ? user.getEmail() : user.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showRoleChangeDialog(user);
                    else toggleUserActive(user);
                })
                .show();
    }

    private void showRoleChangeDialog(User user) {
        String[] roles = {"INSPECTOR", "MANAGER", "ADMIN"};
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Выберите роль")
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                            .update("role", newRole)
                            .addOnSuccessListener(unused -> {
                                user.setRole(newRole);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(requireContext(), "Роль изменена на " + newRole, Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    private void toggleUserActive(User user) {
        boolean newActive = !user.isActive();
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .update("isActive", newActive)
                .addOnSuccessListener(unused -> {
                    user.setActive(newActive);
                    int active = 0, blocked = 0;
                    for (User u : allUsers) { if (u.isActive()) active++; else blocked++; }
                    binding.countActiveUsers.setText(String.valueOf(active));
                    binding.countBlockedUsers.setText(String.valueOf(blocked));
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(),
                            newActive ? "Пользователь разблокирован" : "Пользователь заблокирован",
                            Toast.LENGTH_SHORT).show();
                });
    }
}