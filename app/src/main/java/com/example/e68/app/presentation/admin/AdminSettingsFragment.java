package com.example.e68.app.presentation.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.e68.app.BuildConfig;
import com.example.e68.app.databinding.FragmentAdminSettingsBinding;
import com.example.e68.app.presentation.common.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AdminSettingsFragment extends BaseFragment<FragmentAdminSettingsBinding> {

    @Override
    protected FragmentAdminSettingsBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                          @Nullable ViewGroup container) {
        return FragmentAdminSettingsBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Версия приложения
        binding.tvVersionName.setText(BuildConfig.VERSION_NAME);

        // Экспорт Excel
        binding.rowExportAll.setOnClickListener(v -> {
            showToast("Экспорт Excel — в разработке");
            // TODO: вызвать ExportUseCase или ViewModel
        });

        // Экспорт PDF
        binding.rowExportPdf.setOnClickListener(v -> {
            showToast("Генерация PDF — в разработке");
            // TODO: вызвать PdfReportUseCase или ViewModel
        });

        // Push-уведомления
        binding.switchPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast(isChecked ? "Уведомления включены" : "Уведомления отключены");
            // TODO: сохранить в SharedPreferences или ViewModel
        });
    }
}