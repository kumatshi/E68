package com.example.e68.app.presentation.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.e68.app.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e68.app.databinding.ItemAdminUserBinding;
import com.example.e68.app.domain.entity.User;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder> {

    private List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public AdminUsersAdapter(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<User> newUsers) {
        this.users = newUsers != null ? newUsers : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemAdminUserBinding binding;

        public UserViewHolder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user, OnUserClickListener listener) {
            binding.tvName.setText(user.getName() != null ? user.getName() : user.getEmail());
            binding.tvEmail.setText(user.getEmail());

            // Защита от null role
            String role = user.getRole();
            binding.tvRole.setText(formatRole(role));
            binding.tvRole.setBackgroundColor(getRoleColor(role));

            binding.tvDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "—");

            // Цвет статуса
            if (user.isActive()) {
                binding.tvStatus.setText("Активен");
                binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(R.color.status_resolved));
            } else {
                binding.tvStatus.setText("Заблокирован");
                binding.tvStatus.setTextColor(binding.getRoot().getContext().getColor(R.color.status_open));
            }

            // Цвет роли
            int roleColor = getRoleColor(user.getRole());
            binding.tvRole.setBackgroundColor(roleColor);

            binding.getRoot().setOnClickListener(v -> listener.onUserClick(user));
        }

        private String formatRole(String role) {
            if (role == null) return "ПОЛЬЗОВАТЕЛЬ";
            switch (role) {
                case "ADMIN": return "АДМИНИСТРАТОР";
                case "MANAGER": return "МЕНЕДЖЕР";
                default: return "ИНСПЕКТОР";
            }
        }

        private int getRoleColor(String role) {
            if (role == null) return 0x334CAF50; // зелёный для пользователя по умолчанию
            switch (role) {
                case "ADMIN": return 0x33F44336;    // красный
                case "MANAGER": return 0x33FF9800;  // оранжевый
                default: return 0x334CAF50;         // зелёный
            }
        }
    }
}