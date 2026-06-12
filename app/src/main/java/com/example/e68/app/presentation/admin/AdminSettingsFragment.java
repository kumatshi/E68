package com.example.e68.app.presentation.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.example.e68.app.BuildConfig;
import com.example.e68.app.databinding.FragmentAdminSettingsBinding;
import com.example.e68.app.presentation.common.BaseFragment;
import com.example.e68.app.presentation.export.ExportViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminSettingsFragment extends BaseFragment<FragmentAdminSettingsBinding> {

    private ExportViewModel exportViewModel;

    @Override
    protected FragmentAdminSettingsBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                          @Nullable ViewGroup container) {
        return FragmentAdminSettingsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        exportViewModel = new ViewModelProvider(this).get(ExportViewModel.class);

        // Версия приложения
        binding.tvVersionName.setText(BuildConfig.VERSION_NAME);

        // ─────────────────────────────────────────────────────────
        // 1. ЭКСПОРТ ВСЕХ ДАННЫХ (Excel)
        // ─────────────────────────────────────────────────────────
        binding.rowExportAll.setOnClickListener(v -> {
            showToast("Начинаем экспорт данных...");
            exportViewModel.exportFullData();
        });

        // ─────────────────────────────────────────────────────────
        // 2. ГЕНЕРАЦИЯ PDF-ОТЧЁТА (с графиками)
        // ─────────────────────────────────────────────────────────
        binding.rowExportPdf.setOnClickListener(v -> {
            showToast("Генерация PDF-отчёта...");
            // За последние 30 дней
            exportViewModel.generatePdfReport(getLastMonthDate(), getCurrentDate());
        });

        // ─────────────────────────────────────────────────────────
        // 3. PUSH-УВЕДОМЛЕНИЯ
        // ─────────────────────────────────────────────────────────
        binding.switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePushPreference(isChecked);
            showToast(isChecked ? "Уведомления включены" : "Уведомления отключены");
        });

        // Загружаем сохранённую настройку
        binding.switchPush.setChecked(loadPushPreference());

        // Наблюдаем за результатами экспорта
        observeExportResults();
    }

    private void observeExportResults() {
        // Excel результат
        exportViewModel.getExcelResult().observe(getViewLifecycleOwner(), file -> {
            if (file != null && file.exists()) {
                showSnackbarWithOpen("Excel экспортирован: " + file.getName(), file,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else if (file == null && Boolean.FALSE.equals(exportViewModel.getIsLoading().getValue())) {
                showSnackbar("Ошибка экспорта Excel", false);
            }
        });

        // PDF результат
        exportViewModel.getPdfResult().observe(getViewLifecycleOwner(), file -> {
            if (file != null && file.exists()) {
                showSnackbarWithOpen("PDF отчёт создан: " + file.getName(), file, "application/pdf");
            } else if (file == null && Boolean.FALSE.equals(exportViewModel.getIsLoading().getValue())) {
                showSnackbar("Ошибка генерации PDF", false);
            }
        });

        // Индикатор загрузки
        exportViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.rowExportAll.setEnabled(!loading);
            binding.rowExportPdf.setEnabled(!loading);
        });

        // Ошибки
        exportViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showSnackbar(error, false);
            }
        });
    }

    private void showSnackbarWithOpen(String message, File file, String mimeType) {
        if (getView() == null) return;
        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
        snackbar.setAction("ОТКРЫТЬ", v -> openFile(file, mimeType));
        snackbar.setActionTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        snackbar.show();
    }

    private void showSnackbar(String message, boolean success) {
        if (getView() == null) return;
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void openFile(File file, String mimeType) {
        try {
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

    private String getLastMonthDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -1);
        return sdf.format(cal.getTime());
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private void savePushPreference(boolean enabled) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        prefs.edit().putBoolean("push_enabled", enabled).apply();
    }

    private boolean loadPushPreference() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("push_enabled", true);
    }
}