 package com.example.e68.app.data.report;

import android.content.Context;
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

            PdfReportBuilder builder =
                    new PdfReportBuilder(context);

            builder.generate(file, defects);

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

            ExcelReportBuilder builder =
                    new ExcelReportBuilder();

            builder.generate(file, defects);

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

        File dir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                ),
                REPORTS_DIR
        );

        if (!dir.exists()) {
            dir.mkdirs();
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
}

