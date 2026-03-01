package com.example.e68.app.data.report;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.e68.app.domain.entity.Defect;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFills;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STPatternType;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final String TAG = "ReportGenerator";
    private static final String REPORTS_DIR = "E68Reports";

    private final Context context;

    public ReportGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    // ════════════════════════════════════════════════════════════
    // PDF
    // ════════════════════════════════════════════════════════════

    public String generatePdf(List<Defect> defects) {
        File file = buildFile("pdf");
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(40, 40, 40, 40);

            DeviceRgb primaryColor  = new DeviceRgb(0x00, 0x7A, 0xFF);
            DeviceRgb headerBg      = new DeviceRgb(0x1A, 0x1A, 0x2E);
            DeviceRgb rowAlt        = new DeviceRgb(0xF5, 0xF7, 0xFA);
            DeviceRgb colorOpen     = new DeviceRgb(0xFF, 0x47, 0x57);
            DeviceRgb colorProgress = new DeviceRgb(0xFF, 0xD1, 0x66);
            DeviceRgb colorResolved = new DeviceRgb(0x06, 0xD6, 0xA0);
            DeviceRgb colorRejected = new DeviceRgb(0x88, 0x92, 0xA4);

            Paragraph title = new Paragraph("ОТЧЁТ ПО ДЕФЕКТАМ ДОРОГ")
                    .setFontSize(22).setBold().setFontColor(headerBg)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4);
            doc.add(title);

            String dateStr = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("ru"))
                    .format(new Date());
            doc.add(new Paragraph("Сформирован: " + dateStr)
                    .setFontSize(10).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            long total    = defects.size();
            long open     = defects.stream().filter(d -> "OPEN".equals(d.getStatus())).count();
            long progress = defects.stream().filter(d -> "IN_PROGRESS".equals(d.getStatus())).count();
            long resolved = defects.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count();
            long rejected = defects.stream().filter(d -> "REJECTED".equals(d.getStatus())).count();
            long critical = defects.stream().filter(d -> "CRITICAL".equals(d.getSeverity())).count();

            Table kpi = new Table(UnitValue.createPercentArray(new float[]{1,1,1,1}))
                    .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
            kpi.addCell(kpiCell("Всего",       String.valueOf(total),    primaryColor));
            kpi.addCell(kpiCell("Открыто",     String.valueOf(open),     colorOpen));
            kpi.addCell(kpiCell("Устранено",   String.valueOf(resolved), colorResolved));
            kpi.addCell(kpiCell("Критических", String.valueOf(critical), colorOpen));
            doc.add(kpi);

            doc.add(new Paragraph("Распределение по статусам")
                    .setFontSize(14).setBold().setFontColor(headerBg).setMarginBottom(8));
            String[][] statusData = {
                    {"Открыт",    String.valueOf(open),     pct(open, total)},
                    {"В работе",  String.valueOf(progress), pct(progress, total)},
                    {"Устранено", String.valueOf(resolved), pct(resolved, total)},
                    {"Отклонено", String.valueOf(rejected), pct(rejected, total)},
            };
            doc.add(buildSummaryTable(statusData, headerBg, rowAlt));

            doc.add(new Paragraph("Распределение по серьёзности")
                    .setFontSize(14).setBold().setFontColor(headerBg).setMarginBottom(8).setMarginTop(16));
            long sevLow  = defects.stream().filter(d -> "LOW".equals(d.getSeverity())).count();
            long sevMed  = defects.stream().filter(d -> "MEDIUM".equals(d.getSeverity())).count();
            long sevHigh = defects.stream().filter(d -> "HIGH".equals(d.getSeverity())).count();
            String[][] sevData = {
                    {"Низкая",      String.valueOf(sevLow),   pct(sevLow,  total)},
                    {"Средняя",     String.valueOf(sevMed),   pct(sevMed,  total)},
                    {"Высокая",     String.valueOf(sevHigh),  pct(sevHigh, total)},
                    {"Критическая", String.valueOf(critical), pct(critical, total)},
            };
            doc.add(buildSummaryTable(sevData, headerBg, rowAlt));

            doc.add(new Paragraph("Детальный список дефектов")
                    .setFontSize(14).setBold().setFontColor(headerBg).setMarginTop(20).setMarginBottom(8));
            Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1.5f, 1f, 1f, 1.5f}))
                    .setWidth(UnitValue.createPercentValue(100));
            String[] headers = {"#", "Название", "Адрес", "Тип", "Статус", "Серьёзность"};
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(headerBg).setPadding(6)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            for (int i = 0; i < defects.size(); i++) {
                Defect d = defects.get(i);
                boolean alt = i % 2 == 1;
                DeviceRgb bg = alt ? rowAlt : new DeviceRgb(255, 255, 255);
                table.addCell(dataCell(String.valueOf(i + 1), bg, 8, TextAlignment.CENTER));
                table.addCell(dataCell(d.getTitle() != null ? d.getTitle() : "—", bg, 8, TextAlignment.LEFT));
                table.addCell(dataCell(d.getAddress() != null ? d.getAddress() : "—", bg, 7, TextAlignment.LEFT));
                table.addCell(dataCell(typeShort(d.getType()), bg, 8, TextAlignment.CENTER));
                table.addCell(statusCell(d.getStatus(), colorOpen, colorProgress, colorResolved, colorRejected));
                table.addCell(severityCell(d.getSeverity()));
            }
            doc.add(table);

            doc.add(new Paragraph("\nСистема мониторинга дефектов E68  |  " + dateStr)
                    .setFontSize(8).setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(20));

            doc.close();
            Log.d(TAG, "PDF saved: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "PDF generation failed", e);
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    // EXCEL
    // ════════════════════════════════════════════════════════════

    public String generateExcel(List<Defect> defects) {
        File file = buildFile("xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Передаём wb явно — это исправляет ошибки компиляции getCTXf()/getWorkbook()
            CellStyle titleStyle   = createTitleStyle(wb);
            CellStyle headerStyle  = createHeaderStyle(wb);
            CellStyle dataStyle    = createDataStyle(wb, false);
            CellStyle dataStyleAlt = createDataStyle(wb, true);
            CellStyle numStyle     = createNumStyle(wb);

            // ════════ Лист 1: СВОДКА ════════
            Sheet summary = wb.createSheet("Сводка");
            summary.setColumnWidth(0, 7000);
            summary.setColumnWidth(1, 4000);
            summary.setColumnWidth(2, 4000);

            Row r0 = summary.createRow(0);
            r0.setHeightInPoints(36);
            org.apache.poi.ss.usermodel.Cell c0 = r0.createCell(0);
            c0.setCellValue("ОТЧЁТ ПО ДЕФЕКТАМ ДОРОГ — E68");
            c0.setCellStyle(titleStyle);
            summary.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

            Row r1 = summary.createRow(1);
            r1.createCell(0).setCellValue("Дата формирования:");
            r1.createCell(1).setCellValue(
                    new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru")).format(new Date()));
            Row r2 = summary.createRow(2);
            r2.createCell(0).setCellValue("Всего дефектов:");
            r2.createCell(1).setCellValue(defects.size());
            summary.createRow(3);

            Row hdr = summary.createRow(4);
            hdr.setHeightInPoints(22);
            setCellStyled(hdr, 0, "Показатель", headerStyle);
            setCellStyled(hdr, 1, "Кол-во",     headerStyle);
            setCellStyled(hdr, 2, "Процент",    headerStyle);

            long total    = defects.size();
            long open     = count(defects, "status",   "OPEN");
            long progress = count(defects, "status",   "IN_PROGRESS");
            long resolved = count(defects, "status",   "RESOLVED");
            long rejected = count(defects, "status",   "REJECTED");
            long critical = count(defects, "severity", "CRITICAL");
            long high     = count(defects, "severity", "HIGH");

            Object[][] kpiRows = {
                    {"Открыто",      open,     pct(open, total)},
                    {"В работе",     progress, pct(progress, total)},
                    {"Устранено",    resolved, pct(resolved, total)},
                    {"Отклонено",    rejected, pct(rejected, total)},
                    {"Критических",  critical, pct(critical, total)},
                    {"Высокий риск", high,     pct(high, total)},
            };
            for (int i = 0; i < kpiRows.length; i++) {
                Row row = summary.createRow(5 + i);
                CellStyle s = (i % 2 == 0) ? dataStyle : dataStyleAlt;
                setCellStyled(row, 0, (String) kpiRows[i][0], s);
                org.apache.poi.ss.usermodel.Cell numCell = row.createCell(1);
                numCell.setCellValue((Long) kpiRows[i][1]);
                numCell.setCellStyle(numStyle);
                setCellStyled(row, 2, (String) kpiRows[i][2], s);
            }

            summary.createRow(12);
            Row typeHdr = summary.createRow(13);
            typeHdr.setHeightInPoints(22);
            setCellStyled(typeHdr, 0, "Тип дефекта", headerStyle);
            setCellStyled(typeHdr, 1, "Кол-во",      headerStyle);
            setCellStyled(typeHdr, 2, "Процент",     headerStyle);

            Map<String, Long> byType = defects.stream().collect(
                    Collectors.groupingBy(d -> typeLabel(d.getType()), Collectors.counting()));
            int[] ri = {14};
            byType.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        Row row = summary.createRow(ri[0]++);
                        CellStyle s = (ri[0] % 2 == 0) ? dataStyle : dataStyleAlt;
                        setCellStyled(row, 0, entry.getKey(), s);
                        org.apache.poi.ss.usermodel.Cell nc = row.createCell(1);
                        nc.setCellValue(entry.getValue());
                        nc.setCellStyle(numStyle);
                        setCellStyled(row, 2, pct(entry.getValue(), total), s);
                    });

            // ════════ Лист 2: ВСЕ ДЕФЕКТЫ ════════
            Sheet detail = wb.createSheet("Все дефекты");
            int[] colWidths = {1500, 7000, 6000, 4500, 3500, 3500, 5000, 5000};
            for (int i = 0; i < colWidths.length; i++) detail.setColumnWidth(i, colWidths[i]);

            Row detHdr = detail.createRow(0);
            detHdr.setHeightInPoints(22);
            String[] cols = {"#", "Название", "Адрес", "Тип", "Статус", "Серьёзность", "Создал", "Дата"};
            for (int i = 0; i < cols.length; i++) setCellStyled(detHdr, i, cols[i], headerStyle);

            SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru"));
            for (int i = 0; i < defects.size(); i++) {
                Defect d = defects.get(i);
                Row row = detail.createRow(i + 1);
                row.setHeightInPoints(18);
                CellStyle s = (i % 2 == 0) ? dataStyle : dataStyleAlt;

                org.apache.poi.ss.usermodel.Cell nc = row.createCell(0);
                nc.setCellValue(i + 1);
                nc.setCellStyle(numStyle);
                setCellStyled(row, 1, d.getTitle()    != null ? d.getTitle()    : "—", s);
                setCellStyled(row, 2, d.getAddress()  != null ? d.getAddress()  : "—", s);
                setCellStyled(row, 3, typeLabel(d.getType()), s);
                setCellStyled(row, 4, statusLabel(d.getStatus()), s);
                setCellStyled(row, 5, severityLabel(d.getSeverity()), s);
                setCellStyled(row, 6, d.getCreatedBy() != null ? d.getCreatedBy() : "—", s);
                setCellStyled(row, 7, d.getCreatedAt() > 0
                        ? dateFmt.format(new Date(d.getCreatedAt())) : "—", s);
            }

            // ════════ Лист 3: КРИТИЧЕСКИЕ ════════
            List<Defect> critList = defects.stream()
                    .filter(d -> "CRITICAL".equals(d.getSeverity()) || "HIGH".equals(d.getSeverity()))
                    .collect(Collectors.toList());
            Sheet critSheet = wb.createSheet("Критические");
            critSheet.setColumnWidth(0, 1500);
            critSheet.setColumnWidth(1, 7000);
            critSheet.setColumnWidth(2, 6000);
            critSheet.setColumnWidth(3, 3500);
            critSheet.setColumnWidth(4, 3500);

            Row critHdr = critSheet.createRow(0);
            critHdr.setHeightInPoints(22);
            String[] critCols = {"#", "Название", "Адрес", "Статус", "Серьёзность"};
            for (int i = 0; i < critCols.length; i++) setCellStyled(critHdr, i, critCols[i], headerStyle);

            for (int i = 0; i < critList.size(); i++) {
                Defect d = critList.get(i);
                Row row = critSheet.createRow(i + 1);
                CellStyle s = (i % 2 == 0) ? dataStyle : dataStyleAlt;
                org.apache.poi.ss.usermodel.Cell nc = row.createCell(0);
                nc.setCellValue(i + 1); nc.setCellStyle(numStyle);
                setCellStyled(row, 1, d.getTitle()   != null ? d.getTitle()   : "—", s);
                setCellStyled(row, 2, d.getAddress() != null ? d.getAddress() : "—", s);
                setCellStyled(row, 3, statusLabel(d.getStatus()), s);
                setCellStyled(row, 4, severityLabel(d.getSeverity()), s);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }
            Log.d(TAG, "Excel saved: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Excel generation failed", e);
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    // PDF HELPERS
    // ════════════════════════════════════════════════════════════

    private Cell kpiCell(String label, String value, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(value).setFontSize(28).setBold().setFontColor(color)
                        .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0))
                .add(new Paragraph(label).setFontSize(10).setFontColor(ColorConstants.GRAY)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(color, 2))
                .setPadding(10);
    }

    private Table buildSummaryTable(String[][] data, DeviceRgb headerBg, DeviceRgb rowAlt) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);
        for (String h : new String[]{"Категория", "Кол-во", "Доля"}) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(headerBg).setPadding(5).setTextAlignment(TextAlignment.CENTER));
        }
        for (int i = 0; i < data.length; i++) {
            DeviceRgb bg = i % 2 == 1 ? rowAlt : new DeviceRgb(255,255,255);
            t.addCell(dataCell(data[i][0], bg, 9, TextAlignment.LEFT));
            t.addCell(dataCell(data[i][1], bg, 9, TextAlignment.CENTER));
            t.addCell(dataCell(data[i][2], bg, 9, TextAlignment.CENTER));
        }
        return t;
    }

    private Cell dataCell(String text, DeviceRgb bg, float fontSize, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—").setFontSize(fontSize).setTextAlignment(align))
                .setBackgroundColor(bg).setPadding(4);
    }

    private Cell statusCell(String status,
                            DeviceRgb open, DeviceRgb progress,
                            DeviceRgb resolved, DeviceRgb rejected) {
        DeviceRgb color; String label;
        switch (status != null ? status : "") {
            case "OPEN":        color = open;     label = "Открыт";   break;
            case "IN_PROGRESS": color = progress; label = "В работе"; break;
            case "RESOLVED":    color = resolved; label = "Устранён"; break;
            case "REJECTED":    color = rejected; label = "Отклонён"; break;
            default:            color = rejected; label = "—";        break;
        }
        return new Cell()
                .add(new Paragraph(label).setFontSize(8).setBold().setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(color).setPadding(4);
    }

    private Cell severityCell(String severity) {
        DeviceRgb color; String label;
        switch (severity != null ? severity : "") {
            case "LOW":      color = new DeviceRgb(0x06,0xD6,0xA0); label = "Низкая";    break;
            case "MEDIUM":   color = new DeviceRgb(0xFF,0xD1,0x66); label = "Средняя";   break;
            case "HIGH":     color = new DeviceRgb(0xFF,0x6B,0x35); label = "Высокая";   break;
            case "CRITICAL": color = new DeviceRgb(0xFF,0x47,0x57); label = "Критичная"; break;
            default:         color = new DeviceRgb(0x88,0x92,0xA4); label = "—";         break;
        }
        return new Cell()
                .add(new Paragraph(label).setFontSize(8).setBold().setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(color).setPadding(4);
    }

    // ════════════════════════════════════════════════════════════
    // EXCEL STYLE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Устанавливает цвет заливки ячейки через CTFill напрямую.
     * ИСПРАВЛЕНО: workbook передаётся явным параметром — getCTXf() и getWorkbook()
     * недоступны через интерфейс XSSFCellStyle в данной версии POI.
     */
    private void setCellBgColor(XSSFWorkbook wb, XSSFCellStyle style, int r, int g, int b) {
        byte[] argbBytes = {(byte) 0xFF, (byte) r, (byte) g, (byte) b};
        try {
            CTStylesheet stylesheet = wb.getStylesSource().getCTStylesheet();

            CTFills fills = stylesheet.getFills() != null
                    ? stylesheet.getFills()
                    : stylesheet.addNewFills();

            CTFill newFill = fills.addNewFill();
            CTPatternFill pf = newFill.addNewPatternFill();
            pf.setPatternType(STPatternType.SOLID);
            CTColor fg = pf.addNewFgColor();
            fg.setRgb(argbBytes);

            long fillId = fills.sizeOfFillArray() - 1;
            fills.setCount(fillId + 1);

            // Получаем CTXf через StylesTable — единственный надёжный способ
            org.apache.poi.xssf.model.StylesTable stylesTable = wb.getStylesSource();
            int styleIdx = style.getIndex();
            CTXf ctXf = stylesTable.getCellXfAt(styleIdx);
            ctXf.setFillId(fillId);
            ctXf.setApplyFill(true);

        } catch (Exception e) {
            Log.w(TAG, "setCellBgColor fallback to IndexedColors", e);
            style.setFillForegroundColor(
                    org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        }
    }

    /**
     * Устанавливает цвет шрифта через CTFont/CTColor (OpenXML low-level API).
     * Не использует XSSFFont.setColor() — он тянет java.awt.Color на Android.
     */
    private void setFontColor(XSSFFont font, int r, int g, int b) {
        CTFont ctFont = font.getCTFont();
        CTColor ctColor = ctFont.sizeOfColorArray() > 0
                ? ctFont.getColorArray(0)
                : ctFont.addNewColor();
        ctColor.setRgb(new byte[]{(byte) 0xFF, (byte) r, (byte) g, (byte) b});
    }

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        setFontColor(f, 0x1A, 0x1A, 0x2E);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        setCellBgColor(wb, s, 0x00, 0x7A, 0xFF);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        setFontColor(f, 0xFF, 0xFF, 0xFF);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        return s;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle s = wb.createCellStyle();
        if (alt) {
            setCellBgColor(wb, s, 0xF0, 0xF4, 0xFF);
        }
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderTop(BorderStyle.HAIR);
        return s;
    }

    private CellStyle createNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private void setCellStyled(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ════════════════════════════════════════════════════════════
    // FILE + UTILS
    // ════════════════════════════════════════════════════════════

    private File buildFile(String ext) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), REPORTS_DIR);
        if (!dir.exists()) dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir, "E68_Report_" + ts + "." + ext);
    }

    private String pct(long part, long total) {
        if (total == 0) return "0%";
        return (int) (part * 100 / total) + "%";
    }

    private long count(List<Defect> list, String field, String value) {
        if ("status".equals(field))
            return list.stream().filter(d -> value.equals(d.getStatus())).count();
        if ("severity".equals(field))
            return list.stream().filter(d -> value.equals(d.getSeverity())).count();
        return 0;
    }

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

    private String typeShort(String type) {
        if (type == null) return "—";
        switch (type) {
            case "PH_001": return "Выбоина";
            case "PH_002": return "Колея";
            case "PH_003": return "Тр.поп.";
            case "PH_004": return "Тр.прод.";
            case "PH_005": return "Просадка";
            case "MK_001": return "Люк";
            case "MK_002": return "Бордюр";
            case "SW_001": return "Светофор";
            case "SW_002": return "Знак";
            case "DR_001": return "Ливнев.";
            default:       return type;
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "OPEN":        return "Открыт";
            case "IN_PROGRESS": return "В работе";
            case "RESOLVED":    return "Устранён";
            case "REJECTED":    return "Отклонён";
            default:            return s;
        }
    }

    private String severityLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "LOW":      return "Низкая";
            case "MEDIUM":   return "Средняя";
            case "HIGH":     return "Высокая";
            case "CRITICAL": return "Критическая";
            default:         return s;
        }
    }
}