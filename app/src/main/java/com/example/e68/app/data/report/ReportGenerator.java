package com.example.e68.app.data.report;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.example.e68.app.data.report.builders.ExcelReportBuilder;
import com.example.e68.app.data.report.builders.PdfReportBuilder;
import com.example.e68.app.domain.entity.Defect;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class ReportGenerator {

    private static final String REPORTS_DIR = "E68Reports";

    private final Context context;

    public ReportGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    // =========================================================
    // PDF
    // =========================================================

    public String generatePdf(List<Defect> defects) {

        try {

            File file = buildFile("pdf");

            if (file == null) {
                System.err.println("Не удалось создать файл PDF");
                return null;
            }

            // Создаём директорию перед записью
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    System.err.println("Не удалось создать директорию: " + parentDir.getAbsolutePath());
                    return null;
                }
            }

            PdfReportBuilder builder =
                    new PdfReportBuilder(context);

            builder.generate(file, defects);

            System.out.println("PDF сохранён: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // =========================================================
    // EXCEL
    // =========================================================

    public String generateExcel(List<Defect> defects) {

        try {

            File file = buildFile("xlsx");

            if (file == null) {
                System.err.println("Не удалось создать файл Excel");
                return null;
            }

            // Создаём директорию перед записью
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    System.err.println("Не удалось создать директорию: " + parentDir.getAbsolutePath());
                    return null;
                }
            }

            ExcelReportBuilder builder =
                    new ExcelReportBuilder();

            builder.generate(file, defects);

            System.out.println("Excel сохранён: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // =========================================================
    // FILE
    // =========================================================

    private File buildFile(String ext) {

        File dir = getWritableDirectory();

        if (dir == null) {
            System.err.println("Не удалось получить доступную директорию");
            return null;
        }

        String timestamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

        return new File(
                dir,
                "E68_Report_" + timestamp + "." + ext
        );
    }

    /**
     * Получает доступную для записи директорию
     * Приоритет: Download (есть разрешения) → внутреннее хранилище (fallback)
     */
    private File getWritableDirectory() {

        // Проверяем, есть ли доступ к внешнему хранилищу
        boolean hasStoragePermission = hasStoragePermission();

        if (hasStoragePermission) {
            // Пытаемся использовать папку Download
            try {
                File downloadDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                );
                if (downloadDir != null) {
                    File reportsDir = new File(downloadDir, REPORTS_DIR);
                    System.out.println("Используем папку Download: " + reportsDir.getAbsolutePath());
                    return reportsDir;
                }
            } catch (Exception e) {
                System.err.println("Ошибка доступа к Download: " + e.getMessage());
            }
        }

        // Fallback: внутреннее хранилище приложения (всегда доступно)
        try {
            File appDir = context.getExternalFilesDir(null);
            if (appDir == null) {
                appDir = context.getFilesDir();
            }
            File reportsDir = new File(appDir, REPORTS_DIR);
            System.out.println("Используем внутреннее хранилище: " + reportsDir.getAbsolutePath());
            return reportsDir;
        } catch (Exception e) {
            System.err.println("Ошибка доступа к внутреннему хранилищу: " + e.getMessage());
        }

        // Последний fallback - кэш
        try {
            File cacheDir = context.getCacheDir();
            File reportsDir = new File(cacheDir, REPORTS_DIR);
            System.out.println("Используем кэш: " + reportsDir.getAbsolutePath());
            return reportsDir;
        } catch (Exception e) {
            System.err.println("Ошибка доступа к кэшу: " + e.getMessage());
        }

        return null;
    }

    /**
     * Проверяет наличие разрешений на запись в хранилище
     */
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: проверяем MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else {
            // Android 10 и ниже: проверяем WRITE_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
}