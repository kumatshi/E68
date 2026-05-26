 package com.example.e68.app.data.report.builders;

import android.content.Context;

import com.example.e68.app.data.report.components.TableFactory;
import com.example.e68.app.data.report.theme.ReportTheme;
import com.example.e68.app.data.report.typography.FontManager;
import com.example.e68.app.domain.entity.Defect;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.element.LineSeparator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class PdfReportBuilder {

    private final Context context;
    private final FontManager fonts;
    private final TableFactory tableFactory;

    public PdfReportBuilder(Context context) throws Exception {

        this.context = context;

        fonts = new FontManager(context);

        tableFactory = new TableFactory(fonts);
    }

    public void generate(
            File file,
            List<Defect> defects
    ) throws Exception {

        PdfWriter writer =
                new PdfWriter(new FileOutputStream(file));

        PdfDocument pdf =
                new PdfDocument(writer);

        Document doc =
                new Document(pdf, PageSize.A4);

        doc.setMargins(
                ReportTheme.PAGE_MARGIN_TOP,
                ReportTheme.PAGE_MARGIN_RIGHT,
                ReportTheme.PAGE_MARGIN_BOTTOM,
                ReportTheme.PAGE_MARGIN_LEFT
        );

        addTitlePage(doc, defects);

        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        addRegistry(doc, defects);

        doc.close();
    }

    private void addTitlePage(
            Document doc,
            List<Defect> defects
    ) {

        doc.add(
                new Paragraph("СИСТЕМА МОНИТОРИНГА E68")
                        .setFont(fonts.bold())
                        .setFontSize(11f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(6f)
        );

        doc.add(
                new Paragraph(
                        "Автоматизированная система контроля\n" +
                                "дефектов дорожного покрытия"
                )
                        .setFont(fonts.regular())
                        .setFontSize(10f)
                        .setFontColor(ReportTheme.GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(18f)
        );

        SolidLine line = new SolidLine();
        line.setLineWidth(0.7f);
        doc.add(new LineSeparator(line));

        doc.add(
                new Paragraph("ОТЧЁТ")
                        .setFont(fonts.bold())
                        .setFontSize(24f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(40f)
                        .setMarginBottom(8f)
        );

        doc.add(
                new Paragraph(
                        "о состоянии дорожной инфраструктуры"
                )
                        .setFont(fonts.regular())
                        .setFontSize(12f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(32f)
        );

        Table stats =
                new Table(
                        UnitValue.createPercentArray(
                                new float[]{1,1,1}
                        )
                );

        stats.setWidth(UnitValue.createPercentValue(100));

        stats.addCell(
                tableFactory.header("Всего дефектов")
        );

        stats.addCell(
                tableFactory.header("Открыто")
        );

        stats.addCell(
                tableFactory.header("Критических")
        );

        stats.addCell(
                tableFactory.cell(
                        String.valueOf(defects.size()),
                        false,
                        true
                )
        );

        stats.addCell(
                tableFactory.cell(
                        String.valueOf(
                                defects.stream()
                                        .filter(d ->
                                                "OPEN".equals(
                                                        d.getStatus()
                                                )
                                        )
                                        .count()
                        ),
                        false,
                        true
                )
        );

        stats.addCell(
                tableFactory.cell(
                        String.valueOf(
                                defects.stream()
                                        .filter(d ->
                                                "CRITICAL".equals(
                                                        d.getSeverity()
                                                )
                                        )
                                        .count()
                        ),
                        false,
                        true
                )
        );

        doc.add(stats);
    }

    private void addRegistry(
            Document doc,
            List<Defect> defects
    ) {

        doc.add(
                new Paragraph("РЕЕСТР ДЕФЕКТОВ")
                        .setFont(fonts.bold())
                        .setFontSize(16f)
                        .setMarginBottom(18f)
        );

        Table table =
                new Table(
                        UnitValue.createPercentArray(
                                new float[]{
                                        0.8f,
                                        3.2f,
                                        2.5f,
                                        1.5f,
                                        1.5f
                                }
                        )
                );

        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(
                tableFactory.header("№")
        );

        table.addHeaderCell(
                tableFactory.header("Описание")
        );

        table.addHeaderCell(
                tableFactory.header("Адрес")
        );

        table.addHeaderCell(
                tableFactory.header("Статус")
        );

        table.addHeaderCell(
                tableFactory.header("Серьёзность")
        );

        for (int i = 0; i < defects.size(); i++) {

            Defect d = defects.get(i);

            boolean zebra = i % 2 == 0;

            table.addCell(
                    tableFactory.cell(
                            String.valueOf(i + 1),
                            zebra,
                            true
                    )
            );

            table.addCell(
                    tableFactory.cell(
                            safe(d.getTitle()),
                            zebra,
                            false
                    )
            );

            table.addCell(
                    tableFactory.cell(
                            safe(d.getAddress()),
                            zebra,
                            false
                    )
            );

            table.addCell(
                    tableFactory.cell(
                            safe(d.getStatus()),
                            zebra,
                            true
                    )
            );

            table.addCell(
                    tableFactory.cell(
                            safe(d.getSeverity()),
                            zebra,
                            true
                    )
            );
        }

        doc.add(table);
    }

    private String safe(String text) {
        return text == null || text.isEmpty()
                ? "—"
                : text;
    }
}

