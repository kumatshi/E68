package com.example.e68.app.presentation.analytics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentAnalyticsBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AnalyticsFragment extends BaseFragment<FragmentAnalyticsBinding> {

    private AnalyticsViewModel viewModel;
    private List<Defect> currentDefects = new ArrayList<>();

    @Override
    protected FragmentAnalyticsBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                      @Nullable ViewGroup container) {
        return FragmentAnalyticsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        setupExportButtons();
        observeViewModel();
    }

    // ═══════════════════════════════════════════════════════════
    // КНОПКИ ЭКСПОРТА
    // ═══════════════════════════════════════════════════════════

    private void setupExportButtons() {
        binding.exportPdfBtn.setOnClickListener(v -> {
            if (currentDefects.isEmpty()) {
                showToast("Нет данных для экспорта");
                return;
            }
            viewModel.generatePdf(currentDefects);
        });

        binding.exportExcelBtn.setOnClickListener(v -> {
            if (currentDefects.isEmpty()) {
                showToast("Нет данных для экспорта");
                return;
            }
            viewModel.generateExcel(currentDefects);
        });
    }

    // ═══════════════════════════════════════════════════════════
    // OBSERVERS
    // ═══════════════════════════════════════════════════════════

    private void observeViewModel() {
        viewModel.getDefects().observe(getViewLifecycleOwner(), defects -> {
            if (defects == null) return;
            currentDefects = defects;
            if (!defects.isEmpty()) {
                updateKpi(defects);
                updateStatusChart(defects);
                updateTypeChart(defects);
            }
        });

        // Индикатор загрузки
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            binding.exportPdfBtn.setEnabled(!loading);
            binding.exportExcelBtn.setEnabled(!loading);
            // Небольшой индикатор на кнопках
            binding.exportPdfBtn.setText(loading ? "…" : "PDF");
            binding.exportExcelBtn.setText(loading ? "…" : "Excel");
        });

        // Результат PDF
        viewModel.getPdfResult().observe(getViewLifecycleOwner(), path -> {
            binding.exportPdfBtn.setText("PDF");
            if (path == null) {
                showSnackbar("Ошибка генерации PDF", false);
            } else {
                showSnackbarWithOpen("PDF сохранён в Downloads/E68Reports", path, "application/pdf");
            }
        });

        // Результат Excel
        viewModel.getExcelResult().observe(getViewLifecycleOwner(), path -> {
            binding.exportExcelBtn.setText("Excel");
            if (path == null) {
                showSnackbar("Ошибка генерации Excel", false);
            } else {
                showSnackbarWithOpen("Excel сохранён в Downloads/E68Reports", path,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    // СНЭКБАР С КНОПКОЙ "ОТКРЫТЬ"
    // ═══════════════════════════════════════════════════════════

    private void showSnackbarWithOpen(String message, String filePath, String mimeType) {
        if (getView() == null) return;
        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
        snackbar.setAction("ОТКРЫТЬ", v -> openFile(filePath, mimeType));
        snackbar.setActionTextColor(Color.parseColor("#FFD166"));
        snackbar.show();
    }

    private void showSnackbar(String message, boolean success) {
        if (getView() == null) return;
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void openFile(String filePath, String mimeType) {
        try {
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Открыть файл"));
        } catch (Exception e) {
            showToast("Установите приложение для просмотра файла");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // KPI
    // ═══════════════════════════════════════════════════════════

    private void updateKpi(List<Defect> defects) {
        long total    = defects.size();
        long resolved = defects.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count();
        binding.kpiTotal.setText(String.valueOf(total));
        binding.kpiResolved.setText(String.valueOf(resolved));
    }

    // ═══════════════════════════════════════════════════════════
    // СТАТУСЫ
    // ═══════════════════════════════════════════════════════════

    private void updateStatusChart(List<Defect> defects) {
        binding.statusContainer.removeAllViews();
        int total = defects.size();
        if (total == 0) return;

        String[] statuses = {"OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"};
        String[] labels   = {"Открыто", "В работе", "Устранено", "Отклонено"};
        int[] colors = {
                Color.parseColor("#FF4757"), Color.parseColor("#FFD166"),
                Color.parseColor("#06D6A0"), Color.parseColor("#8892A4")
        };

        for (int i = 0; i < statuses.length; i++) {
            final int idx = i;
            long count = defects.stream().filter(d -> statuses[idx].equals(d.getStatus())).count();
            int pct = (int)(count * 100 / total);

            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_stat_row, binding.statusContainer, false);
            ((TextView) row.findViewById(R.id.statLabel)).setText(labels[i]);
            ((TextView) row.findViewById(R.id.statValue)).setText(count + " (" + pct + "%)");
            ProgressBar pb = row.findViewById(R.id.statProgress);
            pb.setProgress(pct);
            pb.setProgressTintList(android.content.res.ColorStateList.valueOf(colors[i]));
            binding.statusContainer.addView(row);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ТИПЫ
    // ═══════════════════════════════════════════════════════════

    private void updateTypeChart(List<Defect> defects) {
        binding.typeContainer.removeAllViews();
        int total = defects.size();
        if (total == 0) return;

        Map<String, Long> byType = defects.stream()
                .collect(Collectors.groupingBy(
                        d -> typeLabel(d.getType()), Collectors.counting()));

        byType.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .forEach(entry -> {
                    int pct = (int)(entry.getValue() * 100 / total);
                    View row = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_stat_row, binding.typeContainer, false);
                    ((TextView) row.findViewById(R.id.statLabel)).setText(entry.getKey());
                    ((TextView) row.findViewById(R.id.statValue))
                            .setText(entry.getValue() + " (" + pct + "%)");
                    ProgressBar pb = row.findViewById(R.id.statProgress);
                    pb.setProgress(pct);
                    pb.setProgressTintList(android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#FF6B35")));
                    binding.typeContainer.addView(row);
                });
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private String typeLabel(String type) {
        if (type == null) return "Другое";
        switch (type) {
            case "PH_001": return "Выбоина";
            case "PH_002": return "Колея";
            case "PH_003": return "Трещина поп.";
            case "PH_004": return "Трещина прод.";
            case "PH_005": return "Просадка";
            case "MK_001": return "Люк/решётка";
            case "MK_002": return "Бордюр";
            case "SW_001": return "Светофор";
            case "SW_002": return "Знак";
            case "DR_001": return "Ливневая";
            default:       return type;
        }
    }
}