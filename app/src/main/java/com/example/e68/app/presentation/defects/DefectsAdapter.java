package com.example.e68.app.presentation.defects;

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

    public interface OnDefectClickListener { void onClick(Defect defect); }
    private final OnDefectClickListener listener;

    public DefectsAdapter(OnDefectClickListener listener) {
        super(new DiffUtil.ItemCallback<Defect>() {
            @Override public boolean areItemsTheSame(@NonNull Defect o, @NonNull Defect n) { return o.getId() == n.getId(); }
            @Override public boolean areContentsTheSame(@NonNull Defect o, @NonNull Defect n) {
                return o.getStatus().equals(n.getStatus()) && o.getTitle().equals(n.getTitle()) && o.getUpdatedAt() == n.getUpdatedAt();
            }
        });
        this.listener = listener;
        setHasStableIds(true); // Оптимизация RecyclerView
    }

    @Override public long getItemId(int position) { return getItem(position).getId(); }

    @NonNull @Override public DefectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DefectViewHolder(ItemDefectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull DefectViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class DefectViewHolder extends RecyclerView.ViewHolder {
        private final ItemDefectBinding b;
        private static final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());

        DefectViewHolder(ItemDefectBinding b) { super(b.getRoot()); this.b = b; }

        void bind(Defect defect, OnDefectClickListener listener) {
            b.defectTitle.setText(defect.getTitle());
            b.defectAddress.setText(defect.getAddress());
            b.defectDate.setText(fmt.format(new Date(defect.getCreatedAt())));
            StatusHelper.applyStatus(b.statusBadge, defect.getStatus());
            b.getRoot().setOnClickListener(v -> listener.onClick(defect));
        }
    }
}