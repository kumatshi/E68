
        package com.example.e68.app.data.report.theme;

import com.itextpdf.kernel.colors.DeviceGray;

public final class ReportTheme {

    private ReportTheme() {}

    // ===== COLORS =====

    public static final DeviceGray BLACK = new DeviceGray(0f);
    public static final DeviceGray DARK = new DeviceGray(0.15f);
    public static final DeviceGray GRAY = new DeviceGray(0.45f);
    public static final DeviceGray LIGHT = new DeviceGray(0.92f);
    public static final DeviceGray BORDER = new DeviceGray(0.75f);
    public static final DeviceGray WHITE = new DeviceGray(1f);

    // ===== SPACING =====

    public static final float SPACE_XXS = 2f;
    public static final float SPACE_XS = 4f;
    public static final float SPACE_SM = 8f;
    public static final float SPACE_MD = 14f;
    public static final float SPACE_LG = 22f;
    public static final float SPACE_XL = 32f;

    // ===== TYPOGRAPHY =====

    public static final float FONT_CAPTION = 8f;
    public static final float FONT_BODY = 10f;
    public static final float FONT_SUBTITLE = 12f;
    public static final float FONT_TITLE = 20f;

    // ===== TABLE =====

    public static final float TABLE_CELL_PADDING = 6f;
    public static final float TABLE_BORDER_WIDTH = 0.5f;
    public static final float TABLE_ROW_HEIGHT = 24f;

    // ===== PAGE =====

    public static final float PAGE_MARGIN_LEFT = 60f;
    public static final float PAGE_MARGIN_RIGHT = 40f;
    public static final float PAGE_MARGIN_TOP = 55f;
    public static final float PAGE_MARGIN_BOTTOM = 65f;
}
