package com.example.e68.app.presentation.export;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.data.report.AdvancedExcelExporter;
import com.example.e68.app.data.report.PdfReportWithCharts;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ExportViewModel extends AndroidViewModel {

    private static final String TAG = "ExportViewModel";

    private final GetAllDefectsUseCase getAllDefectsUseCase;

    private final MutableLiveData<File> excelResult = new MutableLiveData<>();
    private final MutableLiveData<File> pdfResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<File> getExcelResult() { return excelResult; }
    public LiveData<File> getPdfResult() { return pdfResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    @Inject
    public ExportViewModel(Application app, GetAllDefectsUseCase getAllDefectsUseCase) {
        super(app);
        this.getAllDefectsUseCase = getAllDefectsUseCase;
    }

    public void exportFullData() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        getAllDefectsUseCase.execute().observeForever(defects -> {
            if (defects == null) {
                isLoading.postValue(false);
                errorMessage.postValue("Нет данных для экспорта");
                return;
            }

            // Конвертируем в расширенный формат
            List<AdvancedExcelExporter.DefectExtended> extendedList = new ArrayList<>();
            for (Defect defect : defects) {  // ← ИСПРАВЛЕНО: переменная defect вместо d
                AdvancedExcelExporter.DefectExtended ext = new AdvancedExcelExporter.DefectExtended();
                ext.id = String.valueOf(defect.getId());
                ext.title = defect.getTitle() != null ? defect.getTitle() : "";
                ext.typeLabel = getTypeLabel(defect.getType());
                ext.severity = defect.getSeverity() != null ? defect.getSeverity() : "";
                ext.status = defect.getStatus() != null ? defect.getStatus() : "";
                ext.address = defect.getAddress() != null ? defect.getAddress() : "";
                ext.latitude = defect.getLatitude();
                ext.longitude = defect.getLongitude();
                ext.description = defect.getDescription() != null ? defect.getDescription() : "";
                ext.createdBy = defect.getCreatedBy() != null ? defect.getCreatedBy() : "";
                ext.createdByUid = defect.getLocalUuid() != null ? defect.getLocalUuid() : "";
                ext.createdAtFormatted = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(defect.getCreatedAt()));
                ext.updatedAtFormatted = ext.createdAtFormatted;
                extendedList.add(ext);
            }

            AdvancedExcelExporter exporter = new AdvancedExcelExporter(getApplication());
            File file = exporter.exportDefectsFull(extendedList);
            isLoading.postValue(false);
            excelResult.postValue(file);
        });
    }

    public void generatePdfReport(String startDate, String endDate) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        getAllDefectsUseCase.execute().observeForever(defects -> {
            if (defects == null || defects.isEmpty()) {
                isLoading.postValue(false);
                errorMessage.postValue("Нет данных для отчёта");
                return;
            }

            // Собираем данные для отчёта
            PdfReportWithCharts.ReportData data = new PdfReportWithCharts.ReportData();
            data.periodStart = startDate;
            data.periodEnd = endDate;
            data.generatedAt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
            data.generatedBy = "Система";

            data.totalDefects = defects.size();
            data.openCount = (int) defects.stream().filter(d -> "OPEN".equals(d.getStatus())).count();
            data.inProgressCount = (int) defects.stream().filter(d -> "IN_PROGRESS".equals(d.getStatus())).count();
            data.resolvedCount = (int) defects.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count();

            data.resolvedPercent = data.totalDefects > 0 ? (data.resolvedCount * 100 / data.totalDefects) + "%" : "0%";
            data.inProgressPercent = data.totalDefects > 0 ? (data.inProgressCount * 100 / data.totalDefects) + "%" : "0%";
            data.openPercent = data.totalDefects > 0 ? (data.openCount * 100 / data.totalDefects) + "%" : "0%";

            // Статусы для круговой диаграммы
            data.statusChartData.put("Открыт", data.openCount);
            data.statusChartData.put("В работе", data.inProgressCount);
            data.statusChartData.put("Устранён", data.resolvedCount);

            // Типы для столбчатой диаграммы
            Map<String, Long> typeMap = defects.stream().collect(Collectors.groupingBy(d -> getTypeLabel(d.getType()), Collectors.counting()));
            for (Map.Entry<String, Long> e : typeMap.entrySet()) {
                data.typeChartData.put(e.getKey(), e.getValue().intValue());
            }

            // Серьёзность для гистограммы
            Map<String, Long> severityMap = defects.stream().collect(Collectors.groupingBy(Defect::getSeverity, Collectors.counting()));
            for (Map.Entry<String, Long> e : severityMap.entrySet()) {
                String label = getSeverityLabel(e.getKey());
                data.severityChartData.put(label, e.getValue().intValue());
            }

            // Динамика по дням (упрощённо)
            Map<String, Integer> dailyMap = new HashMap<>();
            for (Defect defect : defects) {
                String day = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(defect.getCreatedAt()));
                dailyMap.put(day, dailyMap.getOrDefault(day, 0) + 1);
            }
            data.dailyChartData = dailyMap;

            PdfReportWithCharts generator = new PdfReportWithCharts(getApplication());
            File file = generator.generateReport(data);
            isLoading.postValue(false);
            pdfResult.postValue(file);
        });
    }

    private String getTypeLabel(String type) {
        if (type == null) return "Другое";
        switch (type) {
            case "PH_001": return "Выбоина";
            case "PH_002": return "Колея";
            case "PH_003": return "Трещина попер.";
            case "PH_004": return "Трещина прод.";
            case "PH_005": return "Просадка";
            case "MK_001": return "Люк/решётка";
            case "MK_002": return "Бордюр";
            case "SW_001": return "Светофор";
            case "SW_002": return "Знак";
            case "DR_001": return "Ливневая";
            default: return type;
        }
    }

    private String getSeverityLabel(String severity) {
        if (severity == null) return "Средняя";
        switch (severity) {
            case "LOW": return "Низкая";
            case "HIGH": return "Высокая";
            case "CRITICAL": return "Критическая";
            default: return "Средняя";
        }
    }
}