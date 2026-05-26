package com.example.e68.app.data.report.builders;

import com.example.e68.app.domain.entity.Defect;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelReportBuilder {

    public void generate(
            File file,
            List<Defect> defects
    ) throws Exception {

        // Создаём родительскую директорию, если её нет
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        XSSFWorkbook workbook =
                new XSSFWorkbook();

        Sheet sheet =
                workbook.createSheet("Дефекты");

        sheet.createFreezePane(0, 1);

        String[] headers = {
                "№",
                "Описание",
                "Адрес",
                "Статус",
                "Серьёзность"
        };

        CellStyle headerStyle =
                createHeaderStyle(workbook);

        CellStyle dataStyle =
                createDataStyle(workbook);

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {

            Cell cell =
                    headerRow.createCell(i);

            cell.setCellValue(headers[i]);

            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < defects.size(); i++) {

            Defect d = defects.get(i);

            Row row = sheet.createRow(i + 1);

            create(row, 0, String.valueOf(i + 1), dataStyle);
            create(row, 1, safe(d.getTitle()), dataStyle);
            create(row, 2, safe(d.getAddress()), dataStyle);
            create(row, 3, safe(d.getStatus()), dataStyle);
            create(row, 4, safe(d.getSeverity()), dataStyle);
        }

        // ❌ УДАЛЕНО: sheet.autoSizeColumn(i);
        // ✅ Вместо autoSizeColumn используем фиксированную ширину колонок
        setColumnWidths(sheet, headers);

        sheet.setAutoFilter(
                new org.apache.poi.ss.util.CellRangeAddress(
                        0,
                        defects.size(),
                        0,
                        headers.length - 1
                )
        );

        FileOutputStream fos =
                new FileOutputStream(file);

        workbook.write(fos);

        fos.close();

        workbook.close();
    }

    /**
     * Устанавливает ширину колонок вручную
     * (замена для autoSizeColumn, который требует AWT и недоступен на Android)
     */
    private void setColumnWidths(Sheet sheet, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            // 256 = ширина одного символа в Excel
            // 15 = минимальная ширина, 50 = максимальная
            int width = calculateColumnWidth(headers[i], i);
            sheet.setColumnWidth(i, width);
        }
    }

    /**
     * Рассчитывает ширину колонки на основе заголовка
     */
    private int calculateColumnWidth(String header, int columnIndex) {
        switch (columnIndex) {
            case 0: // №
                return 5 * 256;
            case 1: // Описание
                return 40 * 256;
            case 2: // Адрес
                return 35 * 256;
            case 3: // Статус
                return 15 * 256;
            case 4: // Серьёзность
                return 15 * 256;
            default:
                return 20 * 256;
        }
    }

    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {

        Font font = workbook.createFont();

        font.setBold(true);

        CellStyle style =
                workbook.createCellStyle();

        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);

        style.setBorderBottom(BorderStyle.THIN);

        return style;
    }

    private CellStyle createDataStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setWrapText(true);

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setBorderBottom(BorderStyle.HAIR);

        return style;
    }

    private void create(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(value);

        cell.setCellStyle(style);
    }

    private String safe(String text) {

        return text == null || text.isEmpty()
                ? "—"
                : text;
    }
}