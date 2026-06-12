package com.example.e68.app.data.report;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.geom.Rectangle;



import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PdfReportWithCharts {

    private static final String TAG = "PdfReportWithCharts";
    private final Context context;

    // ====== Цветовая палитра отчёта (бренд E68) ======
    private static final DeviceRgb COLOR_PRIMARY      = new DeviceRgb(26, 75, 140);   // тёмно-синий
    private static final DeviceRgb COLOR_PRIMARY_LT   = new DeviceRgb(232, 240, 250); // светло-синий фон
    private static final DeviceRgb COLOR_ACCENT       = new DeviceRgb(255, 153, 0);   // оранжевый акцент
    private static final DeviceRgb COLOR_TEXT_MUTED   = new DeviceRgb(110, 120, 135);
    private static final DeviceRgb COLOR_HEADER_BG    = new DeviceRgb(26, 75, 140);
    private static final DeviceRgb COLOR_ROW_ALT      = new DeviceRgb(245, 248, 252);
    private static final DeviceRgb COLOR_DIVIDER      = new DeviceRgb(210, 220, 232);
    private static final DeviceRgb COLOR_SUCCESS      = new DeviceRgb(56, 158, 89);
    private static final DeviceRgb COLOR_WARNING      = new DeviceRgb(230, 162, 60);
    private static final DeviceRgb COLOR_DANGER       = new DeviceRgb(214, 69, 65);

    // Палитра для графиков (согласована с фирменными цветами)
    private static final int[] CHART_PALETTE = new int[]{
            android.graphics.Color.rgb(26, 75, 140),
            android.graphics.Color.rgb(255, 153, 0),
            android.graphics.Color.rgb(56, 158, 89),
            android.graphics.Color.rgb(214, 69, 65),
            android.graphics.Color.rgb(120, 144, 156),
            android.graphics.Color.rgb(149, 117, 205)
    };

    public PdfReportWithCharts(Context context) {
        this.context = context.getApplicationContext();
    }

    public File generateReport(ReportData data) {
        File file = buildFile();
        PdfDocument pdfDoc = null;
        Document doc = null;

        try {
            PdfFont fontRegular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            pdfDoc = new PdfDocument(writer);
            doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(40, 40, 50, 40);

            // ===================== ШАПКА ОТЧЁТА =====================
            addCoverHeader(doc, data, fontRegular, fontBold);

            // ===================== 1. ОБЩАЯ СТАТИСТИКА =====================
            addSectionTitle(doc, "1", "Общая статистика", fontBold);
            addSummaryCards(doc, data, fontRegular, fontBold);
            doc.add(new Paragraph(" ").setFontSize(4));

            // ===================== 2. СТАТУСЫ (PIE) =====================
            addSectionTitle(doc, "2", "Распределение по статусам", fontBold);
            Bitmap pieBitmap = createPieChart(data.statusChartData);
            addChartImage(doc, pieBitmap, "Доля дефектов по текущему статусу");

            // ===================== 3. ТИПЫ (BAR) =====================
            addSectionTitle(doc, "3", "Распределение по типам дефектов", fontBold);
            Bitmap barBitmap = createBarChart(data.typeChartData);
            addChartImage(doc, barBitmap, "Количество дефектов по типам");

            // ===================== 4. СЕРЬЁЗНОСТЬ (BAR) =====================
            addSectionTitle(doc, "4", "Распределение по уровню серьёзности", fontBold);
            Bitmap severityBitmap = createBarChart(data.severityChartData);
            addChartImage(doc, severityBitmap, "Дефекты по степени серьёзности");

            // ===================== 5. ДИНАМИКА (LINE) =====================
            addSectionTitle(doc, "5", "Динамика выявления дефектов по дням", fontBold);
            Bitmap lineBitmap = createLineChart(data.dailyChartData);
            addChartImage(doc, lineBitmap, "Количество новых дефектов по дням периода");

            // ===================== 6. РЕЙТИНГ ИНСПЕКТОРОВ =====================
            addSectionTitle(doc, "6", "Рейтинг инспекторов", fontBold);
            addInspectorTable(doc, data, fontRegular, fontBold);

            // Закрываем документ перед добавлением футера
            doc.close();



            pdfDoc.close();

            // Проверяем, что файл создался
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "PDF created successfully: " + file.getAbsolutePath() +
                        ", size: " + file.length() + " bytes");
            } else {
                Log.e(TAG, "PDF file is empty or not created!");
            }

            return file;
        } catch (Exception e) {
            Log.e(TAG, "PDF generation failed", e);

            // Если произошла ошибка, удаляем пустой файл
            if (file.exists() && file.length() == 0) {
                file.delete();
            }
            return null;
        } finally {
            // Дополнительная страховка - закрываем ресурсы

        }
    }


    // ======================================================================
    // ШАПКА
    // ======================================================================
    private void addCoverHeader(Document doc, ReportData data, PdfFont regular, PdfFont bold) {
        // Цветная плашка-заголовок
        Table headerBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(COLOR_PRIMARY)
                .setMarginBottom(2);

        Cell headerCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(16)
                .setBackgroundColor(COLOR_PRIMARY);

        headerCell.add(new Paragraph("ОТЧЁТ ПО ДЕФЕКТАМ ДОРОГ")
                .setFont(bold)
                .setFontSize(20)
                .setFontColor(new DeviceRgb(255, 255, 255))
                .setMarginBottom(2));

        headerCell.add(new Paragraph("ГУДХ Оренбургской области · Система мониторинга E68")
                .setFont(regular)
                .setFontSize(11)
                .setFontColor(new DeviceRgb(220, 232, 248)));

        headerBar.addCell(headerCell);
        doc.add(headerBar);

        // Оранжевая полоса-акцент под шапкой
        Table accentBar = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        accentBar.addCell(new Cell()
                .setHeight(4)
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_ACCENT));
        doc.add(accentBar);

        // Блок с метаинформацией
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(10)
                .setMarginBottom(14);

        meta.addCell(metaCell("Период отчёта", data.periodStart + " — " + data.periodEnd, regular, bold));
        meta.addCell(metaCell("Дата формирования", data.generatedAt, regular, bold));
        meta.addCell(metaCell("Сформировал", data.generatedBy, regular, bold));
        meta.addCell(metaCell("Всего дефектов в выборке", String.valueOf(data.totalDefects), regular, bold));

        doc.add(meta);
    }

    private Cell metaCell(String label, String value, PdfFont regular, PdfFont bold) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
        cell.add(new Paragraph(label.toUpperCase())
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(COLOR_TEXT_MUTED)
                .setMarginBottom(1));
        cell.add(new Paragraph(value)
                .setFont(bold)
                .setFontSize(11)
                .setFontColor(new DeviceRgb(40, 40, 40)));
        return cell;
    }

    // ======================================================================
    // ЗАГОЛОВКИ РАЗДЕЛОВ
    // ======================================================================
    private void addSectionTitle(Document doc, String number, String title, PdfFont bold) {
        Table titleTable = new Table(UnitValue.createPercentArray(new float[]{0.06f, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(14)
                .setMarginBottom(8);

        Cell numCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(4);
        numCell.add(new Paragraph(number)
                .setFont(bold)
                .setFontColor(new DeviceRgb(255, 255, 255))
                .setFontSize(12)
                .setMultipliedLeading(1f)
                .setTextAlignment(TextAlignment.CENTER));

        Cell textCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPaddingLeft(8);
        textCell.add(new Paragraph(title.toUpperCase())
                .setFont(bold)
                .setFontSize(13)
                .setFontColor(new DeviceRgb(40, 40, 40)));

        titleTable.addCell(numCell);
        titleTable.addCell(textCell);
        doc.add(titleTable);

        // тонкая линия-разделитель
        doc.add(new LineSeparator(new SolidLine(0.75f))
                .setStrokeColor(COLOR_DIVIDER)
                .setMarginBottom(8));
    }

    // ======================================================================
    // КАРТОЧКИ СВОДНОЙ СТАТИСТИКИ
    // ======================================================================
    private void addSummaryCards(Document doc, ReportData data, PdfFont regular, PdfFont bold) {
        Table cards = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        cards.addCell(statCard("Всего дефектов", String.valueOf(data.totalDefects),
                null, COLOR_PRIMARY, regular, bold));
        cards.addCell(statCard("Устранено", data.resolvedCount + "",
                data.resolvedPercent + "%", COLOR_SUCCESS, regular, bold));
        cards.addCell(statCard("В работе", data.inProgressCount + "",
                data.inProgressPercent + "%", COLOR_WARNING, regular, bold));
        cards.addCell(statCard("Открыто", data.openCount + "",
                data.openPercent + "%", COLOR_DANGER, regular, bold));

        doc.add(cards);
    }

    private Cell statCard(String label, String value, String subValue, DeviceRgb accent,
                          PdfFont regular, PdfFont bold) {
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_PRIMARY_LT)
                .setBorder(Border.NO_BORDER)
                .setBorderLeft(new SolidBorder(accent, 3))
                .setPadding(10)
                .setMarginRight(4)
                .setTextAlignment(TextAlignment.LEFT);

        cell.add(new Paragraph(label.toUpperCase())
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(COLOR_TEXT_MUTED)
                .setMarginBottom(3));

        Paragraph valueP = new Paragraph()
                .add(new com.itextpdf.layout.element.Text(value)
                        .setFont(bold)
                        .setFontSize(20)
                        .setFontColor(new DeviceRgb(40, 40, 40)));

        if (subValue != null) {
            valueP.add(new com.itextpdf.layout.element.Text("  " + subValue)
                    .setFont(bold)
                    .setFontSize(11)
                    .setFontColor(accent));
        }
        cell.add(valueP);
        return cell;
    }

    // ======================================================================
    // ВСТАВКА ГРАФИКОВ
    // ======================================================================
    private void addChartImage(Document doc, Bitmap bitmap, String caption) throws IOException {
        if (bitmap == null) {
            doc.add(new Paragraph("Нет данных для построения графика.")
                    .setFontSize(9)
                    .setFontColor(COLOR_TEXT_MUTED)
                    .setItalic());
            return;
        }
        Table wrapper = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell imgCell = new Cell()
                .setBorder(new SolidBorder(COLOR_DIVIDER, 0.75f))
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(ColorConstants.WHITE);

        Image image = new Image(com.itextpdf.io.image.ImageDataFactory.create(bitmapToBytes(bitmap)));
        image.setWidth(UnitValue.createPercentValue(100));
        image.setHorizontalAlignment(HorizontalAlignment.CENTER);
        imgCell.add(image);

        if (caption != null) {
            imgCell.add(new Paragraph(caption)
                    .setFontSize(8)
                    .setFontColor(COLOR_TEXT_MUTED)
                    .setItalic()
                    .setMarginTop(4)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        wrapper.addCell(imgCell);
        doc.add(wrapper);
        doc.add(new Paragraph(" ").setFontSize(4));
    }

    // ======================================================================
    // ТАБЛИЦА РЕЙТИНГА ИНСПЕКТОРОВ
    // ======================================================================
    private void addInspectorTable(Document doc, ReportData data, PdfFont regular, PdfFont bold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1f, 1.5f}))
                .setWidth(UnitValue.createPercentValue(100));

        // Заголовок таблицы
        table.addHeaderCell(headerCell("№", bold, TextAlignment.CENTER));
        table.addHeaderCell(headerCell("ФИО инспектора", bold, TextAlignment.LEFT));
        table.addHeaderCell(headerCell("Дефектов", bold, TextAlignment.CENTER));
        table.addHeaderCell(headerCell("Ср. время передачи (ч)", bold, TextAlignment.CENTER));

        int idx = 1;
        for (InspectorRating r : data.inspectorRatings) {
            boolean alt = idx % 2 == 0;
            DeviceRgb bg = alt ? COLOR_ROW_ALT : new DeviceRgb(255, 255, 255);

            table.addCell(bodyCell(String.valueOf(idx), regular, TextAlignment.CENTER, bg));
            table.addCell(bodyCell(r.name, regular, TextAlignment.LEFT, bg));
            table.addCell(bodyCell(String.valueOf(r.defectCount), regular, TextAlignment.CENTER, bg));
            table.addCell(bodyCell(String.format(Locale.getDefault(), "%.1f", r.avgReactionHours),
                    regular, TextAlignment.CENTER, bg));
            idx++;
        }

        if (data.inspectorRatings.isEmpty()) {
            Cell empty = new Cell(1, 4)
                    .setBorder(new SolidBorder(COLOR_DIVIDER, 0.5f))
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER);
            empty.add(new Paragraph("Нет данных по инспекторам за выбранный период")
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(COLOR_TEXT_MUTED)
                    .setItalic());
            table.addCell(empty);
        }

        doc.add(table);
    }

    private Cell headerCell(String text, PdfFont bold, TextAlignment align) {
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(6)
                .setTextAlignment(align);
        cell.add(new Paragraph(text)
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(new DeviceRgb(255, 255, 255)));
        return cell;
    }

    private Cell bodyCell(String text, PdfFont regular, TextAlignment align, DeviceRgb bg) {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(COLOR_DIVIDER, 0.5f))
                .setPadding(6)
                .setTextAlignment(align);
        cell.add(new Paragraph(text)
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(new DeviceRgb(50, 50, 50)));
        return cell;
    }

    // ======================================================================
    // ФУТЕР (номера страниц + подпись)
    // ======================================================================
    private void addFooter(Document doc, PdfDocument pdfDoc, PdfFont regular) {
        try {
            int pages = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                PdfPage page = pdfDoc.getPage(i);
                if (page == null) continue;

                com.itextpdf.kernel.geom.Rectangle pageSize = page.getPageSize();
                if (pageSize == null) continue;

                float x = pageSize.getWidth() / 2;
                float y = 20;

                com.itextpdf.layout.Canvas canvas = new com.itextpdf.layout.Canvas(page, pageSize);
                canvas.showTextAligned(
                        new Paragraph("Сформировано автоматически системой E68 · Страница " + i + " из " + pages)
                                .setFont(regular)
                                .setFontSize(7)
                                .setFontColor(COLOR_TEXT_MUTED),
                        x, y, TextAlignment.CENTER);
                canvas.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding footer", e);
        }
    }

    // ======================================================================
    // ГРАФИКИ MPAndroidChart
    // ======================================================================
    private Bitmap createPieChart(java.util.Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return null;

        PieChart chart = new PieChart(context);
        chart.setLayoutParams(new ViewGroup.LayoutParams(900, 600));
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setHoleRadius(45f);
        chart.setTransparentCircleRadius(50f);
        chart.setDrawEntryLabels(true);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(android.graphics.Color.WHITE);
        chart.setExtraOffsets(20, 10, 20, 10);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);

        java.util.ArrayList<PieEntry> entries = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> colors = new java.util.ArrayList<>();
        int i = 0;
        for (java.util.Map.Entry<String, Integer> e : data.entrySet()) {
            entries.add(new PieEntry(e.getValue(), e.getKey()));
            colors.add(CHART_PALETTE[i % CHART_PALETTE.length]);
            i++;
        }
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new com.github.mikephil.charting.formatter.PercentFormatter(chart));
        chart.setData(pieData);
        chart.invalidate();

        return renderToBitmap(chart, 900, 600);
    }

    private Bitmap createBarChart(java.util.Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return null;

        BarChart chart = new BarChart(context);
        chart.setLayoutParams(new ViewGroup.LayoutParams(900, 550));
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(10, 10, 10, 10);
        chart.getLegend().setEnabled(false);
        chart.setFitBars(true);

        java.util.ArrayList<BarEntry> entries = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> colors = new java.util.ArrayList<>();
        int idx = 0;
        for (java.util.Map.Entry<String, Integer> e : data.entrySet()) {
            entries.add(new BarEntry(idx, e.getValue()));
            labels.add(e.getKey());
            colors.add(CHART_PALETTE[idx % CHART_PALETTE.length]);
            idx++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(11f);
        xAxis.setLabelRotationAngle(labels.size() > 5 ? -30f : 0f);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setTextSize(11f);
        chart.getAxisRight().setEnabled(false);

        chart.setData(new BarData(dataSet));
        chart.invalidate();

        return renderToBitmap(chart, 900, 550);
    }

    private Bitmap createLineChart(java.util.Map<String, Integer> data) {
        if (data == null || data.isEmpty()) return null;

        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new ViewGroup.LayoutParams(900, 550));
        chart.getDescription().setEnabled(false);
        chart.setExtraOffsets(10, 10, 10, 10);
        chart.getLegend().setEnabled(false);

        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        int idx = 0;
        for (java.util.Map.Entry<String, Integer> e : data.entrySet()) {
            entries.add(new Entry(idx, e.getValue()));
            labels.add(e.getKey());
            idx++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(CHART_PALETTE[0]);
        dataSet.setCircleColor(CHART_PALETTE[1]);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(CHART_PALETTE[0]);
        dataSet.setFillAlpha(40);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(labels.size() > 7 ? -45f : 0f);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setTextSize(11f);
        chart.getAxisRight().setEnabled(false);

        chart.setData(new LineData(dataSet));
        chart.invalidate();

        return renderToBitmap(chart, 900, 550);
    }

    private Bitmap renderToBitmap(View view, int width, int height) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(android.graphics.Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private File buildFile() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "E68Reports");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
            }
        }
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir, "E68_Report_" + ts + ".pdf");
    }

    // ====================== DTO ======================
    public static class ReportData {
        public String periodStart, periodEnd, generatedAt, generatedBy;
        public int totalDefects, resolvedCount, inProgressCount, openCount;
        public String resolvedPercent, inProgressPercent, openPercent;
        public java.util.Map<String, Integer> statusChartData = new java.util.LinkedHashMap<>();
        public java.util.Map<String, Integer> typeChartData = new java.util.LinkedHashMap<>();
        public java.util.Map<String, Integer> severityChartData = new java.util.LinkedHashMap<>();
        public java.util.Map<String, Integer> dailyChartData = new java.util.LinkedHashMap<>();
        public java.util.ArrayList<InspectorRating> inspectorRatings = new java.util.ArrayList<>();
    }

    public static class InspectorRating {
        public String name;
        public int defectCount;
        public double avgReactionHours;
    }
}