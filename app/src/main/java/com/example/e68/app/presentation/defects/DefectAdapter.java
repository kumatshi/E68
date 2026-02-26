package com.example.e68.app.presentation.defects;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.e68.app.databinding.ItemDefectBinding;
import com.example.e68.app.domain.entity.Defect;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DefectsAdapter extends ListAdapter<Defect, DefectsAdapter.DefectViewHolder> {

    public interface OnDefectClickListener {
        void onClick(Defect defect);
    }

    private final OnDefectClickListener listener;

    public DefectsAdapter(OnDefectClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Defect> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Defect o, @NonNull Defect n) { return o.getId() == n.getId(); }
        @Override
        public boolean areContentsTheSame(@NonNull Defect o, @NonNull Defect n) {
            return o.getStatus().equals(n.getStatus()) && o.getTitle().equals(n.getTitle());
        }
    };

    @NonNull
    @Override
    public DefectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDefectBinding b = ItemDefectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DefectViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull DefectViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class DefectViewHolder extends RecyclerView.ViewHolder {
        private final ItemDefectBinding b;

        DefectViewHolder(ItemDefectBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Defect defect, OnDefectClickListener listener) {
            b.defectTitle.setText(defect.getTitle());
            b.defectAddress.setText(defect.getAddress() != null ? defect.getAddress() : "Координаты: " +
                    String.format("%.4f, %.4f", defect.getLatitude(), defect.getLongitude()));
            b.defectCreatedBy.setText(defect.getCreatedBy() != null ? defect.getCreatedBy() : "—");
            b.defectDate.setText(new SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                    .format(new Date(defect.getCreatedAt())));

            // Type chip
            b.typeChip.setText(getTypeLabel(defect.getType()));

            // Status badge
            StatusHelper.applyStatus(b.statusBadge, defect.getStatus());

            // Severity dot color
            b.severityDot.getBackground().mutate().setTint(getSeverityColor(defect.getSeverity()));

            // Sync icon
            b.syncIcon.setVisibility(defect.isSynced() ? android.view.View.GONE : android.view.View.VISIBLE);

            b.getRoot().setOnClickListener(v -> listener.onClick(defect));
        }

        private int getSeverityColor(String severity) {
            switch (severity != null ? severity : "") {
                case "LOW":      return Color.parseColor("#06D6A0");
                case "MEDIUM":   return Color.parseColor("#FFD166");
                case "HIGH":     return Color.parseColor("#FF6B35");
                case "CRITICAL": return Color.parseColor("#FF4757");
                default:         return Color.parseColor("#8892A4");
            }
        }

        private String getTypeLabel(String type) {
            if (type == null) return "Неизвестно";
            switch (type) {
                case "PH_001": return "Выбоина";
                case "PH_002": return "Колея";
                case "PH_003": return "Трещина попер.";
                case "PH_004": return "Трещина прод.";
                case "PH_005": return "Просадка";
                case "MK_001": return "Люк / решётка";
                case "MK_002": return "Бортовой камень";
                case "SW_001": return "Светофор";
                case "SW_002": return "Дорожный знак";
                case "DR_001": return "Ливневая канал.";
                default:        return type;
            }
        }
    }
}
