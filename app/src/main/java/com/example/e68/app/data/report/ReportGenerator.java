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
     * Использует внутреннее хранилище приложения (всегда доступно без разрешений)
     */
    private File getWritableDirectory() {

        // Пытаемся использовать внутреннее хранилище приложения (не требует разрешений)
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

        // Fallback - кэш приложения
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
}