package com.example.e68.app.presentation.manager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e68.app.R;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.defects.StatusHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManagerDefectsAdapter extends RecyclerView.Adapter<ManagerDefectsAdapter.ViewHolder> {

    private List<Defect> defects = new ArrayList<>();
    private final OnDefectActionListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public interface OnDefectActionListener {
        void onEdit(Defect defect);
        void onDelete(Defect defect);
        void onView(Defect defect);
    }

    public ManagerDefectsAdapter(OnDefectActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Defect> newList) {
        this.defects = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manager_defect, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Defect defect = defects.get(position);
        holder.bind(defect, listener);
    }

    @Override
    public int getItemCount() {
        return defects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvAddress;
        private final TextView tvDate;
        private final TextView tvSeverity;
        private final TextView tvStatus;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDefectTitle);
            tvAddress = itemView.findViewById(R.id.tvDefectAddress);
            tvDate = itemView.findViewById(R.id.tvDefectDate);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Defect defect, OnDefectActionListener listener) {
            tvTitle.setText(defect.getTitle() != null ? defect.getTitle() : "Без названия");
            tvAddress.setText(defect.getAddress() != null ? defect.getAddress() : "Адрес не указан");
            tvDate.setText(dateFormat.format(new Date(defect.getCreatedAt())));

            // Серьёзность
            String severity = defect.getSeverity() != null ? defect.getSeverity() : "MEDIUM";
            tvSeverity.setText(getSeverityLabel(severity));
            tvSeverity.setTextColor(getSeverityColor(severity));

            // Статус
            StatusHelper.applyStatus(tvStatus, defect.getStatus());

            // Клик по карточке - просмотр
            itemView.setOnClickListener(v -> listener.onView(defect));

            // Кнопка редактирования
            btnEdit.setOnClickListener(v -> listener.onEdit(defect));

            // Кнопка удаления
            btnDelete.setOnClickListener(v -> listener.onDelete(defect));
        }

        private String getSeverityLabel(String severity) {
            switch (severity) {
                case "LOW": return "Низкая";
                case "MEDIUM": return "Средняя";
                case "HIGH": return "Высокая";
                case "CRITICAL": return "Критичная";
                default: return severity;
            }
        }

        private int getSeverityColor(String severity) {
            switch (severity) {
                case "LOW": return 0xFF06D6A0;
                case "MEDIUM": return 0xFFFFD166;
                case "HIGH": return 0xFFFF6B35;
                case "CRITICAL": return 0xFFFF4757;
                default: return 0xFF8892A4;
            }
        }
    }
}