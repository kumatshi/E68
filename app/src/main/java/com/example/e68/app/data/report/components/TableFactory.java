package com.example.e68.app.data.report.components;

import com.example.e68.app.data.report.theme.ReportTheme;
import com.example.e68.app.data.report.typography.FontManager;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

public class TableFactory {

    private final FontManager fonts;

    public TableFactory(FontManager fonts) {
        this.fonts = fonts;
    }

    public Cell header(String text) {

        return new Cell()
                .add(
                        new Paragraph(text)
                                .setFont(fonts.bold())
                                .setFontSize(9f)
                                .setFontColor(ReportTheme.WHITE)
                                .setTextAlignment(TextAlignment.CENTER)
                )
                .setBackgroundColor(ReportTheme.DARK)
                .setPadding(ReportTheme.TABLE_CELL_PADDING)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(
                        new SolidBorder(
                                ReportTheme.BLACK,
                                ReportTheme.TABLE_BORDER_WIDTH
                        )
                )
                .setMinHeight(28f);
    }

    public Cell cell(String text, boolean zebra, boolean center) {

        return new Cell()
                .add(
                        new Paragraph(text)
                                .setFont(fonts.regular())
                                .setFontSize(9f)
                                .setFontColor(ReportTheme.BLACK)
                                .setTextAlignment(
                                        center
                                                ? TextAlignment.CENTER
                                                : TextAlignment.LEFT
                                )
                                .setMultipliedLeading(1.15f)
                )
                .setPadding(ReportTheme.TABLE_CELL_PADDING)
                .setBackgroundColor(
                        zebra
                                ? ReportTheme.LIGHT
                                : ReportTheme.WHITE
                )
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(
                        new SolidBorder(
                                ReportTheme.BORDER,
                                ReportTheme.TABLE_BORDER_WIDTH
                        )
                )
                .setKeepTogether(true)
                .setMinHeight(ReportTheme.TABLE_ROW_HEIGHT);
    }
}

