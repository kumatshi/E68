package com.example.e68.app.data.report;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.e68.app.domain.entity.Defect;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
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

/**
 * ReportGenerator
 * Формирует официальный отчёт в стиле государственного документа (ГОСТ-подобный).
 * Оформление: строгое, чёрно-белое, минимум цвета.
 */
public class ReportGenerator {

    private static final String TAG         = "ReportGenerator";
    private static final String REPORTS_DIR = "E68Reports";
    private static final String ORG_NAME    = "Система мониторинга дефектов дорог E68";

    // ── Цвета: только чёрный, белый и оттенки серого ─────────
    private static final DeviceGray BLACK      = new DeviceGray(0f);
    private static final DeviceGray DARK_GRAY  = new DeviceGray(0.15f);
    private static final DeviceGray MID_GRAY   = new DeviceGray(0.45f);
    private static final DeviceGray LIGHT_GRAY = new DeviceGray(0.85f);
    private static final DeviceGray ROW_ALT    = new DeviceGray(0.95f);
    private static final DeviceGray WHITE      = new DeviceGray(1f);

    private final Context context;

    public ReportGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    // ════════════════════════════════════════════════════════════
    // PDF — ТОЧКА ВХОДА
    // ════════════════════════════════════════════════════════════

    public String generatePdf(List<Defect> defects) {
        File file = buildFile("pdf");
        try {
            PdfWriter   writer = new PdfWriter(new FileOutputStream(file));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document    doc    = new Document(pdfDoc, PageSize.A4);
            // Поля по ГОСТ: левое 30мм, правое 15мм, верхнее 20мм, нижнее 20мм
            doc.setMargins(57, 43, 57, 85);

            String docNum  = new SimpleDateFormat("yyyyMMdd-HHmm",
                    Locale.getDefault()).format(new Date());
            String dateRu  = new SimpleDateFormat("«dd» MMMM yyyy г.",
                    new Locale("ru")).format(new Date());
            String timeRu  = new SimpleDateFormat("HH:mm",
                    Locale.getDefault()).format(new Date());

            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE,
                    new PageFooterHandler(pdfDoc, docNum));

            // ── РАЗДЕЛЫ ───────────────────────────────────────
            addTitleBlock(doc, docNum, dateRu, timeRu, defects);
            addSectionHeading(doc, "1. ОБЩИЕ СВЕДЕНИЯ");
            addGeneralInfo(doc, defects, dateRu, timeRu, docNum);
            addSectionHeading(doc, "2. СВЕДЕНИЯ О СОСТОЯНИИ ДЕФЕКТОВ");
            addStatusTable(doc, defects);
            addSectionHeading(doc, "3. РАСПРЕДЕЛЕНИЕ ПО СТЕПЕНИ СЕРЬЁЗНОСТИ");
            addSeverityTable(doc, defects);
            addSectionHeading(doc, "4. РЕЕСТР ДЕФЕКТОВ");
            addDefectRegistry(doc, defects);
            addSignatureBlock(doc, dateRu);

            doc.close();
            Log.d(TAG, "PDF saved: " + file.getAbsolutePath());
            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "PDF generation failed", e);
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    // ТИТУЛЬНЫЙ БЛОК
    // ════════════════════════════════════════════════════════════

    private void addTitleBlock(Document doc, String docNum,
                               String dateRu, String timeRu,
                               List<Defect> defects) {

        // Верхний реквизит организации
        doc.add(new Paragraph(ORG_NAME.toUpperCase())
                .setFontSize(10).setBold().setFontColor(BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2));

        doc.add(new Paragraph("Автоматизированная система учёта и мониторинга\n"
                + "дефектов дорожного покрытия")
                .setFontSize(9).setFontColor(MID_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(14));

        // Горизонтальная линия
        addLine(doc, 1.5f);

        // Реквизиты документа — правый блок (ГОСТ)
        doc.add(new Paragraph(" ")
                .setFontSize(6).setMarginBottom(4));

        Table reqs = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(4);
        reqs.addCell(reqCell("", false));
        reqs.addCell(reqCell("Документ № " + docNum, true));
        reqs.addCell(reqCell("", false));
        reqs.addCell(reqCell("Дата составления: " + dateRu, true));
        reqs.addCell(reqCell("", false));
        reqs.addCell(reqCell("Время формирования: " + timeRu, true));
        doc.add(reqs);

        // Главный заголовок по центру
        doc.add(new Paragraph(" ").setFontSize(8));
        doc.add(new Paragraph("О Т Ч Ё Т")
                .setFontSize(18).setBold().setFontColor(BLACK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(6));
        doc.add(new Paragraph("о состоянии дефектов дорожного покрытия")
                .setFontSize(12).setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));
        doc.add(new Paragraph("по состоянию на " + dateRu + " " + timeRu)
                .setFontSize(10).setFontColor(MID_GRAY).setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        addLine(doc, 0.5f);
        doc.add(new Paragraph(" ").setFontSize(6));

        // Краткая статистика под заголовком
        long total    = defects.size();
        long open     = count(defects, "status",   "OPEN");
        long resolved = count(defects, "status",   "RESOLVED");
        long critical = count(defects, "severity", "CRITICAL");

        Table summary = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        summary.addCell(summaryCard("Всего дефектов",  String.valueOf(total)));
        summary.addCell(summaryCard("Открыто",         String.valueOf(open)));
        summary.addCell(summaryCard("Устранено",       String.valueOf(resolved)));
        summary.addCell(summaryCard("Критических",     String.valueOf(critical)));
        doc.add(summary);
    }

    // ════════════════════════════════════════════════════════════
    // ОБЩИЕ СВЕДЕНИЯ
    // ════════════════════════════════════════════════════════════

    private void addGeneralInfo(Document doc, List<Defect> defects,
                                String dateRu, String timeRu, String docNum) {

        long total    = defects.size();
        long open     = count(defects, "status",   "OPEN");
        long progress = count(defects, "status",   "IN_PROGRESS");
        long resolved = count(defects, "status",   "RESOLVED");
        long rejected = count(defects, "status",   "REJECTED");
        long critical = count(defects, "severity", "CRITICAL");
        long high     = count(defects, "severity", "HIGH");
        long urgent   = critical + high;

        // Параграф-аннотация
        addBodyText(doc,
                "Настоящий отчёт (документ № " + docNum + ") сформирован автоматически "
                        + "системой мониторинга дефектов E68 " + dateRu + " в " + timeRu + ". "
                        + "Документ содержит сводную информацию о техническом состоянии "
                        + "дорожного покрытия на дату составления.");

        addBodyText(doc,
                "По состоянию на дату формирования отчёта в системе зафиксировано "
                        + total + " дефект" + plural(total, "а", "ов", "ов") + " дорожного покрытия. "
                        + "Из них " + open + " (" + pct(open, total) + ") имеют статус «Открыт» "
                        + "и ожидают назначения ответственных исполнителей; "
                        + progress + " (" + pct(progress, total) + ") находятся в работе; "
                        + resolved + " (" + pct(resolved, total) + ") устранены и закрыты; "
                        + rejected + " (" + pct(rejected, total) + ") отклонены как "
                        + "недостоверные или дублирующие.");

        if (urgent > 0) {
            addBodyText(doc,
                    "Из общего числа зафиксированных дефектов " + urgent
                            + " (" + pct(urgent, total) + ") относятся к категориям "
                            + "«Критический» и «Высокий» по степени серьёзности. "
                            + "Указанные объекты требуют первоочерёдного устранения "
                            + "в соответствии с действующим регламентом.");
        }

        doc.add(new Paragraph(" ").setFontSize(6));
    }

    // ════════════════════════════════════════════════════════════
    // ТАБЛИЦА СТАТУСОВ
    // ════════════════════════════════════════════════════════════

    private void addStatusTable(Document doc, List<Defect> defects) {
        long total    = defects.size();
        long open     = count(defects, "status", "OPEN");
        long progress = count(defects, "status", "IN_PROGRESS");
        long resolved = count(defects, "status", "RESOLVED");
        long rejected = count(defects, "status", "REJECTED");

        Table t = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3, 1.2f, 1.2f, 3.5f}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);

        // Шапка
        String[] heads = {"№", "Статус дефекта", "Количество,\nед.", "Доля,\n%", "Пояснение"};
        for (String h : heads) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(9).setBold()
                            .setFontColor(WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(DARK_GRAY)
                    .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(4).setPaddingRight(4)
                    .setBorder(new SolidBorder(BLACK, 0.5f)));
        }

        // Данные
        String[][] rows = {
                {"1", "Открыт (OPEN)",              String.valueOf(open),     pct(open,     total), "Ожидает назначения ответственного исполнителя"},
                {"2", "В работе (IN_PROGRESS)",     String.valueOf(progress), pct(progress, total), "Исполнитель назначен, устранение ведётся"},
                {"3", "Устранён (RESOLVED)",        String.valueOf(resolved), pct(resolved, total), "Работы завершены, дефект закрыт в системе"},
                {"4", "Отклонён (REJECTED)",        String.valueOf(rejected), pct(rejected, total), "Признан недостоверным или дублирующим"},
        };
        for (int i = 0; i < rows.length; i++) {
            DeviceGray bg = (i % 2 == 0) ? WHITE : ROW_ALT;
            t.addCell(tableCell(rows[i][0], bg, TextAlignment.CENTER, true));
            t.addCell(tableCell(rows[i][1], bg, TextAlignment.LEFT,   false));
            t.addCell(tableCell(rows[i][2], bg, TextAlignment.CENTER, true));
            t.addCell(tableCell(rows[i][3], bg, TextAlignment.CENTER, false));
            t.addCell(tableCell(rows[i][4], bg, TextAlignment.LEFT,   false));
        }

        // Итого
        t.addCell(new Cell(1, 2)
                .add(new Paragraph("ИТОГО").setFontSize(9).setBold().setFontColor(BLACK))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(6)
                .setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph(String.valueOf(total)).setFontSize(9).setBold()
                        .setFontColor(BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph("100%").setFontSize(9).setBold()
                        .setFontColor(BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph("").setFontSize(9))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(BLACK, 0.5f)));

        doc.add(t);

        addCaption(doc, "Таблица 1 — Сведения о состоянии дефектов дорожного покрытия");
        doc.add(new Paragraph(" ").setFontSize(6));
    }

    // ════════════════════════════════════════════════════════════
    // ТАБЛИЦА СЕРЬЁЗНОСТИ
    // ════════════════════════════════════════════════════════════

    private void addSeverityTable(Document doc, List<Defect> defects) {
        long total    = defects.size();
        long critical = count(defects, "severity", "CRITICAL");
        long high     = count(defects, "severity", "HIGH");
        long medium   = count(defects, "severity", "MEDIUM");
        long low      = count(defects, "severity", "LOW");

        Table t = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1.2f, 1.2f, 4}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);

        String[] heads = {"№", "Степень серьёзности", "Количество,\nед.", "Доля,\n%",
                "Нормативный срок устранения"};
        for (String h : heads) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(9).setBold()
                            .setFontColor(WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(DARK_GRAY)
                    .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(4).setPaddingRight(4)
                    .setBorder(new SolidBorder(BLACK, 0.5f)));
        }

        String[][] rows = {
                {"1", "Критический (CRITICAL)", String.valueOf(critical), pct(critical, total),
                        "Немедленное устранение (угроза безопасности)"},
                {"2", "Высокий (HIGH)",          String.valueOf(high),     pct(high,     total),
                        "Не более 3 (трёх) рабочих суток"},
                {"3", "Средний (MEDIUM)",         String.valueOf(medium),   pct(medium,   total),
                        "Не более 7 (семи) рабочих суток"},
                {"4", "Низкий (LOW)",             String.valueOf(low),      pct(low,      total),
                        "Плановое устранение в порядке очерёдности"},
        };
        for (int i = 0; i < rows.length; i++) {
            DeviceGray bg = (i % 2 == 0) ? WHITE : ROW_ALT;
            for (int col = 0; col < rows[i].length; col++) {
                TextAlignment align = (col == 0 || col == 2 || col == 3)
                        ? TextAlignment.CENTER : TextAlignment.LEFT;
                t.addCell(tableCell(rows[i][col], bg, align, col == 0 || col == 2 || col == 3));
            }
        }

        // Итого
        t.addCell(new Cell(1, 2)
                .add(new Paragraph("ИТОГО").setFontSize(9).setBold().setFontColor(BLACK))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setPaddingLeft(6).setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph(String.valueOf(total)).setFontSize(9).setBold()
                        .setFontColor(BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph("100%").setFontSize(9).setBold()
                        .setFontColor(BLACK).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(LIGHT_GRAY).setPaddingTop(6).setPaddingBottom(6)
                .setBorder(new SolidBorder(BLACK, 0.5f)));
        t.addCell(new Cell()
                .add(new Paragraph("").setFontSize(9))
                .setBackgroundColor(LIGHT_GRAY)
                .setBorder(new SolidBorder(BLACK, 0.5f)));

        doc.add(t);
        addCaption(doc, "Таблица 2 — Распределение дефектов по степени серьёзности");
        doc.add(new Paragraph(" ").setFontSize(6));
    }

    // ════════════════════════════════════════════════════════════
    // РЕЕСТР ДЕФЕКТОВ
    // ════════════════════════════════════════════════════════════

    private void addDefectRegistry(Document doc, List<Defect> defects) {
        addBodyText(doc,
                "В нижеследующей таблице приведён полный перечень дефектов, "
                        + "зарегистрированных в системе на дату составления отчёта.");

        Table t = new Table(UnitValue.createPercentArray(
                new float[]{0.5f, 2.8f, 2.2f, 1.4f, 1.5f, 1.6f}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);

        String[] heads = {"№", "Наименование дефекта", "Адрес (местоположение)",
                "Тип дефекта", "Статус", "Серьёзность"};
        for (String h : heads) {
            t.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFontSize(8).setBold()
                            .setFontColor(WHITE).setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(DARK_GRAY)
                    .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(4).setPaddingRight(4)
                    .setBorder(new SolidBorder(BLACK, 0.5f)));
        }

        for (int i = 0; i < defects.size(); i++) {
            Defect d = defects.get(i);
            DeviceGray bg = (i % 2 == 0) ? WHITE : ROW_ALT;

            t.addCell(tableCell(String.valueOf(i + 1),    bg, TextAlignment.CENTER, true));
            t.addCell(tableCell(safe(d.getTitle()),        bg, TextAlignment.LEFT,   false));
            t.addCell(tableCell(safe(d.getAddress()),      bg, TextAlignment.LEFT,   false));
            t.addCell(tableCell(typeLabel(d.getType()),    bg, TextAlignment.CENTER, false));
            t.addCell(tableCell(statusLabel(d.getStatus()),bg, TextAlignment.CENTER, false));
            t.addCell(tableCell(severityLabel(d.getSeverity()), bg, TextAlignment.CENTER, false));
        }

        doc.add(t);
        addCaption(doc, "Таблица 3 — Реестр дефектов дорожного покрытия");
        doc.add(new Paragraph(" ").setFontSize(8));
    }

    // ════════════════════════════════════════════════════════════
    // ПОДПИСИ
    // ════════════════════════════════════════════════════════════

    private void addSignatureBlock(Document doc, String dateRu) {
        addLine(doc, 0.5f);
        doc.add(new Paragraph(" ").setFontSize(8));

        doc.add(new Paragraph(
                "Отчёт сформирован автоматически. Дата формирования: " + dateRu + ".")
                .setFontSize(9).setFontColor(MID_GRAY).setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(24));

        // Подписи
        Table sig = new Table(UnitValue.createPercentArray(new float[]{1, 0.5f, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(4);
        sig.addCell(sigCell("Ответственный исполнитель:", false));
        sig.addCell(sigCell("", false));
        sig.addCell(sigCell("Руководитель подразделения:", false));
        sig.addCell(sigCell("________________", true));
        sig.addCell(sigCell("", false));
        sig.addCell(sigCell("________________", true));
        sig.addCell(sigCell("(подпись)", true));
        sig.addCell(sigCell("", false));
        sig.addCell(sigCell("(подпись)", true));
        doc.add(sig);
    }

    // ════════════════════════════════════════════════════════════
    // КОЛОНТИТУЛ
    // ════════════════════════════════════════════════════════════

    private static class PageFooterHandler implements IEventHandler {
        private final PdfDocument pdfDoc;
        private final String docNum;

        PageFooterHandler(PdfDocument pdfDoc, String docNum) {
            this.pdfDoc = pdfDoc;
            this.docNum = docNum;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent e    = (PdfDocumentEvent) event;
            PdfPage          page = e.getPage();
            Rectangle        rect = page.getPageSize();
            int              num  = pdfDoc.getPageNumber(page);
            float            y    = 22f;

            try {
                // Тонкая линия
                PdfCanvas raw = new PdfCanvas(
                        page.newContentStreamBefore(), page.getResources(), pdfDoc);
                raw.setStrokeColor(new DeviceGray(0.5f))
                        .setLineWidth(0.3f)
                        .moveTo(85, y + 12)
                        .lineTo(rect.getWidth() - 43, y + 12)
                        .stroke();
                raw.release();

                Canvas canvas = new Canvas(new PdfCanvas(pdfDoc, num),
                        new Rectangle(85, y, rect.getWidth() - 128, 12));
                canvas.add(new Paragraph("Документ № " + docNum + "  |  " + ORG_NAME)
                        .setFontSize(7).setFontColor(new DeviceGray(0.5f))
                        .setTextAlignment(TextAlignment.LEFT));
                canvas.add(new Paragraph("Стр. " + num)
                        .setFontSize(7).setFontColor(new DeviceGray(0.5f))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFixedPosition(85, y, rect.getWidth() - 128));
                canvas.close();
            } catch (Exception ignored) {}
        }
    }

    // ════════════════════════════════════════════════════════════
    // UI-ПРИМИТИВЫ
    // ════════════════════════════════════════════════════════════

    private void addSectionHeading(Document doc, String text) {
        doc.add(new Paragraph(text)
                .setFontSize(11).setBold().setFontColor(BLACK)
                .setMarginTop(16).setMarginBottom(8));
        addLine(doc, 0.5f);
        doc.add(new Paragraph(" ").setFontSize(4));
    }

    private void addLine(Document doc, float width) {
        doc.add(new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .addCell(new Cell().setHeight(width)
                        .setBackgroundColor(DARK_GRAY)
                        .setBorder(Border.NO_BORDER)));
    }

    private void addBodyText(Document doc, String text) {
        doc.add(new Paragraph(text)
                .setFontSize(10).setFontColor(BLACK)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setFirstLineIndent(28)
                .setMarginBottom(8));
    }

    private void addCaption(Document doc, String text) {
        doc.add(new Paragraph(text)
                .setFontSize(9).setFontColor(MID_GRAY).setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4).setMarginBottom(4));
    }

    private Cell summaryCard(String label, String value) {
        Cell c = new Cell()
                .setBorder(new SolidBorder(DARK_GRAY, 1))
                .setBackgroundColor(ROW_ALT)
                .setPadding(10).setMargin(3);
        c.add(new Paragraph(value)
                .setFontSize(22).setBold().setFontColor(BLACK)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2));
        c.add(new Paragraph(label)
                .setFontSize(8).setFontColor(MID_GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        return c;
    }

    private Cell tableCell(String text, DeviceGray bg, TextAlignment align, boolean bold) {
        Paragraph p = new Paragraph(text).setFontSize(9).setFontColor(BLACK)
                .setTextAlignment(align);
        if (bold) p.setBold();
        return new Cell()
                .add(p).setBackgroundColor(bg)
                .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(5).setPaddingRight(5)
                .setBorder(new SolidBorder(new DeviceGray(0.6f), 0.5f));
    }

    private Cell reqCell(String text, boolean right) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9).setFontColor(DARK_GRAY)
                        .setTextAlignment(right ? TextAlignment.RIGHT : TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER);
    }

    private Cell sigCell(String text, boolean center) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9).setFontColor(DARK_GRAY)
                        .setTextAlignment(center ? TextAlignment.CENTER : TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER).setPaddingBottom(4);
    }

    // ════════════════════════════════════════════════════════════
    // EXCEL (без изменений в логике)
    // ════════════════════════════════════════════════════════════

    public String generateExcel(List<Defect> defects) {
        File file = buildFile("xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            CellStyle titleStyle   = createTitleStyle(wb);
            CellStyle headerStyle  = createHeaderStyle(wb);
            CellStyle dataStyle    = createDataStyle(wb, false);
            CellStyle dataStyleAlt = createDataStyle(wb, true);
            CellStyle numStyle     = createNumStyle(wb);

            Sheet summary = wb.createSheet("Сводка");
            summary.setColumnWidth(0, 7000);
            summary.setColumnWidth(1, 4000);
            summary.setColumnWidth(2, 4000);

            Row r0 = summary.createRow(0); r0.setHeightInPoints(36);
            org.apache.poi.ss.usermodel.Cell c0 = r0.createCell(0);
            c0.setCellValue("ОТЧЁТ ПО ДЕФЕКТАМ ДОРОГ — E68");
            c0.setCellStyle(titleStyle);
            summary.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
            summary.createRow(1).createCell(0).setCellValue(
                    "Дата: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru")).format(new Date()));
            summary.createRow(2).createCell(0).setCellValue("Всего: " + defects.size());
            summary.createRow(3);

            Row hdr = summary.createRow(4); hdr.setHeightInPoints(22);
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
                    {"Открыто",      open,     pct(open,     total)},
                    {"В работе",     progress, pct(progress, total)},
                    {"Устранено",    resolved, pct(resolved, total)},
                    {"Отклонено",    rejected, pct(rejected, total)},
                    {"Критических",  critical, pct(critical, total)},
                    {"Высокий риск", high,     pct(high,     total)},
            };
            for (int i = 0; i < kpiRows.length; i++) {
                Row row = summary.createRow(5 + i);
                CellStyle s = (i % 2 == 0) ? dataStyle : dataStyleAlt;
                setCellStyled(row, 0, (String) kpiRows[i][0], s);
                org.apache.poi.ss.usermodel.Cell nc = row.createCell(1);
                nc.setCellValue((Long) kpiRows[i][1]); nc.setCellStyle(numStyle);
                setCellStyled(row, 2, (String) kpiRows[i][2], s);
            }

            Sheet detail = wb.createSheet("Все дефекты");
            int[] cw = {1500, 7000, 6000, 4500, 3500, 3500, 5000, 5000};
            for (int i = 0; i < cw.length; i++) detail.setColumnWidth(i, cw[i]);
            Row dh = detail.createRow(0); dh.setHeightInPoints(22);
            String[] dcols = {"#", "Название", "Адрес", "Тип", "Статус",
                    "Серьёзность", "Создал", "Дата"};
            for (int i = 0; i < dcols.length; i++) setCellStyled(dh, i, dcols[i], headerStyle);

            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru"));
            for (int i = 0; i < defects.size(); i++) {
                Defect d  = defects.get(i);
                Row row   = detail.createRow(i + 1); row.setHeightInPoints(18);
                CellStyle s = (i % 2 == 0) ? dataStyle : dataStyleAlt;
                org.apache.poi.ss.usermodel.Cell nc = row.createCell(0);
                nc.setCellValue(i + 1); nc.setCellStyle(numStyle);
                setCellStyled(row, 1, safe(d.getTitle()),   s);
                setCellStyled(row, 2, safe(d.getAddress()), s);
                setCellStyled(row, 3, typeLabel(d.getType()), s);
                setCellStyled(row, 4, statusLabel(d.getStatus()), s);
                setCellStyled(row, 5, severityLabel(d.getSeverity()), s);
                setCellStyled(row, 6, safe(d.getCreatedBy()), s);
                setCellStyled(row, 7, d.getCreatedAt() > 0
                        ? df.format(new Date(d.getCreatedAt())) : "—", s);
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
    // EXCEL HELPERS
    // ════════════════════════════════════════════════════════════

    private void setCellBgColor(XSSFWorkbook wb, XSSFCellStyle style, int r, int g, int b) {
        byte[] argb = {(byte) 0xFF, (byte) r, (byte) g, (byte) b};
        try {
            CTStylesheet ss = wb.getStylesSource().getCTStylesheet();
            CTFills fills   = ss.getFills() != null ? ss.getFills() : ss.addNewFills();
            CTFill  fill    = fills.addNewFill();
            CTPatternFill pf = fill.addNewPatternFill();
            pf.setPatternType(STPatternType.SOLID);
            CTColor fg = pf.addNewFgColor(); fg.setRgb(argb);
            long id = fills.sizeOfFillArray() - 1; fills.setCount(id + 1);
            CTXf xf = wb.getStylesSource().getCellXfAt(style.getIndex());
            xf.setFillId(id); xf.setApplyFill(true);
        } catch (Exception e) {
            Log.w(TAG, "BgColor fallback", e);
            style.setFillForegroundColor(
                    org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        }
    }

    private void setFontColor(XSSFFont font, int r, int g, int b) {
        CTFont  cf = font.getCTFont();
        CTColor cc = cf.sizeOfColorArray() > 0 ? cf.getColorArray(0) : cf.addNewColor();
        cc.setRgb(new byte[]{(byte) 0xFF, (byte) r, (byte) g, (byte) b});
    }

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14);
        setFontColor(f, 0x1B, 0x1B, 0x1B); s.setFont(f);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        setCellBgColor(wb, s, 0x26, 0x26, 0x26);
        XSSFFont f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 10);
        setFontColor(f, 0xFF, 0xFF, 0xFF); s.setFont(f);
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        return s;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle s = wb.createCellStyle();
        if (alt) setCellBgColor(wb, s, 0xF2, 0xF2, 0xF2);
        s.setBorderBottom(BorderStyle.HAIR); s.setBorderTop(BorderStyle.HAIR);
        return s;
    }

    private CellStyle createNumStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private void setCellStyled(Row row, int col, String value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value); cell.setCellStyle(style);
    }

    // ════════════════════════════════════════════════════════════
    // УТИЛИТЫ
    // ════════════════════════════════════════════════════════════

    private File buildFile(String ext) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), REPORTS_DIR);
        if (!dir.exists()) dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir, "E68_Report_" + ts + "." + ext);
    }

    private String safe(String s) { return s != null ? s : "—"; }

    private String pct(long part, long total) {
        if (total == 0) return "0%";
        return (int) (part * 100.0 / total) + "%";
    }

    private long count(List<Defect> list, String field, String value) {
        if ("status".equals(field))
            return list.stream().filter(d -> value.equals(d.getStatus())).count();
        if ("severity".equals(field))
            return list.stream().filter(d -> value.equals(d.getSeverity())).count();
        return 0;
    }

    private String plural(long n, String f1, String f2, String f5) {
        long m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return f1;
        if (m10 >= 2 && m10 <= 4 && (m100 < 10 || m100 >= 20)) return f2;
        return f5;
    }

    private String typeLabel(String t) {
        if (t == null) return "Другое";
        switch (t) {
            case "PH_001": return "Выбоина";        case "PH_002": return "Колея";
            case "PH_003": return "Трещина поп.";   case "PH_004": return "Трещина прод.";
            case "PH_005": return "Просадка";       case "MK_001": return "Люк/решётка";
            case "MK_002": return "Бордюр";         case "SW_001": return "Светофор";
            case "SW_002": return "Знак";           case "DR_001": return "Ливневая";
            default: return t;
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "OPEN":        return "Открыт";
            case "IN_PROGRESS": return "В работе";
            case "RESOLVED":    return "Устранён";
            case "REJECTED":    return "Отклонён";
            default: return s;
        }
    }

    private String severityLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "LOW":      return "Низкая";
            case "MEDIUM":   return "Средняя";
            case "HIGH":     return "Высокая";
            case "CRITICAL": return "Критическая";
            default: return s;
        }
    }
}