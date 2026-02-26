package com.example.e68.app.presentation.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentAnalyticsBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AndroidEntryPoint
public class AnalyticsFragment extends BaseFragment<FragmentAnalyticsBinding> {

    private AnalyticsViewModel viewModel;

    @Override
    protected FragmentAnalyticsBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentAnalyticsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        binding.exportPdfBtn.setOnClickListener(v -> {
            showToast("Генерация PDF...");
            viewModel.generatePdf();
        });
        binding.exportExcelBtn.setOnClickListener(v -> {
            showToast("Генерация Excel...");
            viewModel.generateExcel();
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getDefects().observe(getViewLifecycleOwner(), defects -> {
            if (defects == null || defects.isEmpty()) return;
            updateKpi(defects);
            updateStatusChart(defects);
            updateTypeChart(defects);
        });
    }

    private void updateKpi(List<Defect> defects) {
        long total = defects.size();
        long resolved = defects.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count();
        binding.kpiTotal.setText(String.valueOf(total));
        binding.kpiResolved.setText(String.valueOf(resolved));
    }

    private void updateStatusChart(List<Defect> defects) {
        binding.statusContainer.removeAllViews();
        int total = defects.size();
        if (total == 0) return;

        String[] statuses = {"OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"};
        String[] labels = {"Открыто", "В работе", "Устранено", "Отклонено"};
        int[] colors = {
                Color.parseColor("#FF4757"), Color.parseColor("#FFD166"),
                Color.parseColor("#06D6A0"), Color.parseColor("#8892A4")
        };

        for (int i = 0; i < statuses.length; i++) {
            final int idx = i;
            long count = defects.stream().filter(d -> statuses[idx].equals(d.getStatus())).count();
            int pct = (int) (count * 100 / total);

            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stat_row,
                    binding.statusContainer, false);
            ((TextView) row.findViewById(R.id.statLabel)).setText(labels[i]);
            ((TextView) row.findViewById(R.id.statValue)).setText(count + " (" + pct + "%)");
            ProgressBar pb = row.findViewById(R.id.statProgress);
            pb.setProgress(pct);
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(colors[i]));
            binding.statusContainer.addView(row);
        }
    }

    private void updateTypeChart(List<Defect> defects) {
        binding.typeContainer.removeAllViews();
        Map<String, Long> byType = defects.stream()
                .collect(Collectors.groupingBy(d -> d.getType() != null ? d.getType() : "OTHER", Collectors.counting()));

        int total = defects.size();
        byType.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .forEach(entry -> {
                    int pct = (int) (entry.getValue() * 100 / total);
                    View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_stat_row,
                            binding.typeContainer, false);
                    ((TextView) row.findViewById(R.id.statLabel)).setText(entry.getKey());
                    ((TextView) row.findViewById(R.id.statValue)).setText(entry.getValue() + " (" + pct + "%)");
                    ProgressBar pb = row.findViewById(R.id.statProgress);
                    pb.setProgress(pct);
                    pb.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF6B35")));
                    binding.typeContainer.addView(row);
                });
    }
}
