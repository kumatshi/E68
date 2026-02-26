// FILE: app/src/main/java/com/example/e68/app/presentation/admin/UsersAdapter.java
package com.example.e68.app.presentation.admin;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e68.app.R;
import com.example.e68.app.domain.entity.User;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

    public interface OnUserClickListener {
        void onClick(User user);
    }

    private List<User> users;
    private final OnUserClickListener listener;

    public UsersAdapter(List<User> users, OnUserClickListener listener) {
        this.users    = users;
        this.listener = listener;
    }

    public void setUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        User u = users.get(position);

        // Инициалы в аватаре
        String initials = buildInitials(u.getName());
        h.tvInitials.setText(initials);

        // Имя или email
        h.tvName.setText(!u.getName().isEmpty() ? u.getName() : u.getEmail());
        h.tvEmail.setText(u.getEmail());
        h.tvDepartment.setText(u.getDepartment().isEmpty() ? "Подразделение не указано" : u.getDepartment());

        // Роль — бейдж
        h.tvRole.setText(roleToLabel(u.getRole()));
        setRoleBadge(h.tvRole, u.getRole());

        // Статус
        h.tvStatus.setText(u.isActive() ? "Активен" : "Заблокирован");
        h.tvStatus.setTextColor(u.isActive()
                ? h.itemView.getContext().getColor(R.color.status_resolved)
                : h.itemView.getContext().getColor(R.color.status_open));

        h.card.setOnClickListener(v -> listener.onClick(u));
    }

    @Override
    public int getItemCount() { return users.size(); }

    private String buildInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
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

    private void setRoleBadge(TextView tv, String role) {
        int strokeColor;
        int fillColor;
        switch (role != null ? role : "") {
            case "ADMIN":
                strokeColor = 0xFFFF4757; fillColor = 0x33FF4757; break;
            case "MANAGER":
                strokeColor = 0xFFFFD166; fillColor = 0x33FFD166; break;
            default:
                strokeColor = 0xFFFF6B35; fillColor = 0x33FF6B35; break;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(40f);
        bg.setColor(fillColor);
        bg.setStroke(2, strokeColor);
        tv.setBackground(bg);
        tv.setTextColor(strokeColor);
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvInitials, tvName, tvEmail, tvDepartment, tvRole, tvStatus;

        VH(@NonNull View v) {
            super(v);
            card         = v.findViewById(R.id.userCard);
            tvInitials   = v.findViewById(R.id.tvUserInitials);
            tvName       = v.findViewById(R.id.tvUserName);
            tvEmail      = v.findViewById(R.id.tvUserEmail);
            tvDepartment = v.findViewById(R.id.tvUserDepartment);
            tvRole       = v.findViewById(R.id.tvUserRole);
            tvStatus     = v.findViewById(R.id.tvUserStatus);
        }
    }
}