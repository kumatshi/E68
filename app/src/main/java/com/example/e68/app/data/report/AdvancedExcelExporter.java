package com.example.e68.app.data.report;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdvancedExcelExporter {

    private static final String TAG = "AdvancedExcelExporter";
    private static final String EXPORT_DIR = "E68Exports";

    private final Context context;

    public AdvancedExcelExporter(Context context) {
        this.context = context.getApplicationContext();
    }

    public File exportDefectsFull(List<DefectExtended> defects) {
        File file = buildFile("defects_full.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Дефекты");

            // Заголовки
            String[] headers = {
                    "ID", "Название", "Тип дефекта", "Серьёзность", "Статус",
                    "Адрес", "Широта", "Долгота", "Описание", "Фото URL",
                    "Создал", "UID создателя", "Дата создания", "Дата обновления"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Данные
            int rowNum = 1;
            for (DefectExtended d : defects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(d.id);
                row.createCell(1).setCellValue(d.title);
                row.createCell(2).setCellValue(d.typeLabel);
                row.createCell(3).setCellValue(d.severity);
                row.createCell(4).setCellValue(d.status);
                row.createCell(5).setCellValue(d.address);
                row.createCell(6).setCellValue(d.latitude);
                row.createCell(7).setCellValue(d.longitude);
                row.createCell(8).setCellValue(d.description);
                row.createCell(9).setCellValue(d.photoUrl != null ? d.photoUrl : "");
                row.createCell(10).setCellValue(d.createdBy);
                row.createCell(11).setCellValue(d.createdByUid);
                row.createCell(12).setCellValue(d.createdAtFormatted);
                row.createCell(13).setCellValue(d.updatedAtFormatted);
            }

            // Фиксированная ширина вместо autoSizeColumn (AWT не доступен на Android)
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 5000);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            Log.d(TAG, "Excel saved: " + file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return null;
        }
    }

    /**
     * Экспорт статистики (отдельный файл)
     */
    public File exportStatistics(StatisticsData stats) {
        File file = buildFile("statistics.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Лист 1: Общая статистика
            Sheet sheet1 = wb.createSheet("Общая статистика");
            int rowNum = 0;
            rowNum = addStatRow(sheet1, rowNum, "Всего дефектов за период", stats.totalPeriod);
            rowNum = addStatRow(sheet1, rowNum, "Всего дефектов за всё время", stats.totalAllTime);
            rowNum = addStatRow(sheet1, rowNum + 1, "Распределение по статусам", "");
            for (Map.Entry<String, Integer> e : stats.statusDistribution.entrySet()) {
                rowNum = addStatRow(sheet1, rowNum, "  " + e.getKey(), e.getValue());
            }
            rowNum = addStatRow(sheet1, rowNum + 1, "Распределение по серьёзности", "");
            for (Map.Entry<String, Integer> e : stats.severityDistribution.entrySet()) {
                rowNum = addStatRow(sheet1, rowNum, "  " + e.getKey(), e.getValue());
            }
            rowNum = addStatRow(sheet1, rowNum + 1, "Средние показатели", "");
            rowNum = addStatRow(sheet1, rowNum, "Среднее время реакции", stats.avgReactionTimeHours + " ч");
            rowNum = addStatRow(sheet1, rowNum, "Среднее время устранения", stats.avgResolutionTimeDays + " дн");
            rowNum = addStatRow(sheet1, rowNum, "% устранённых дефектов", stats.resolvedPercent + "%");
            rowNum = addStatRow(sheet1, rowNum, "% просроченных (>7 дней)", stats.overduePercent + "%");

            // Лист 2: Распределение по типам
            Sheet sheet2 = wb.createSheet("По типам");
            Row typeHeader = sheet2.createRow(0);
            typeHeader.createCell(0).setCellValue("Тип дефекта");
            typeHeader.createCell(1).setCellValue("Количество");
            int typeRow = 1;
            for (Map.Entry<String, Integer> e : stats.typeDistribution.entrySet()) {
                Row r = sheet2.createRow(typeRow++);
                r.createCell(0).setCellValue(e.getKey());
                r.createCell(1).setCellValue(e.getValue());
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Stats export failed", e);
            return null;
        }
    }

    /**
     * Экспорт пользователей (только для администратора)
     */
    public File exportUsers(List<UserExtended> users) {
        File file = buildFile("users.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Пользователи");
            String[] headers = {"UID", "ФИО", "Email", "Роль", "Подразделение", "Статус", "Дата регистрации", "Кол-во дефектов", "Последняя активность"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (UserExtended u : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(u.uid);
                row.createCell(1).setCellValue(u.name);
                row.createCell(2).setCellValue(u.email);
                row.createCell(3).setCellValue(u.role);
                row.createCell(4).setCellValue(u.department);
                row.createCell(5).setCellValue(u.isActive ? "Активен" : "Заблокирован");
                row.createCell(6).setCellValue(u.registrationDate);
                row.createCell(7).setCellValue(u.defectCount);
                row.createCell(8).setCellValue(u.lastActive);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Users export failed", e);
            return null;
        }
    }

    private int addStatRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value.toString());
        return rowNum + 1;
    }

    private File buildFile(String name) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR);
        if (!dir.exists()) dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir, ts + "_" + name);
    }

    // Вспомогательные DTO
    public static class DefectExtended {
        public String id, title, typeLabel, severity, status, address, description, photoUrl;
        public double latitude, longitude;
        public String createdBy, createdByUid, createdAtFormatted, updatedAtFormatted;
        public String assignedTo, district;
        public double reactionTimeHours, resolutionTimeDays;
    }

    public static class StatisticsData {
        public int totalPeriod, totalAllTime;
        public Map<String, Integer> statusDistribution;
        public Map<String, Integer> severityDistribution;
        public Map<String, Integer> typeDistribution;
        public double avgReactionTimeHours, avgResolutionTimeDays;
        public double resolvedPercent, overduePercent;
    }

    public static class UserExtended {
        public String uid, name, email, role, department, registrationDate, lastActive;
        public boolean isActive;
        public int defectCount;
    }
}