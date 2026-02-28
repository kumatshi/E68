package com.example.e68.app.presentation.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Кастомная View для кластера дефектов.
 * Рисует цветной круг с количеством и цвет определяется
 * по наиболее критичному типу в кластере.
 */
public class DefectClusterView extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int clusterSize = 0;
    private int dominantColor = 0xFFFFD166;

    private static final int VIEW_SIZE_DP = 48;
    private int viewSizePx;

    public DefectClusterView(Context context) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        viewSizePx = (int) (VIEW_SIZE_DP * density);

        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStrokeWidth(3f * density);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(14f * density);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * Заполняем данными кластера.
     * @param types список типов маркеров в кластере
     */
    public void setData(List<DefectMarkerType> types) {
        clusterSize = types.size();

        // Определяем доминирующий цвет — по самому критичному типу
        Map<DefectMarkerType, Integer> counts = new HashMap<>();
        for (DefectMarkerType t : DefectMarkerType.values()) counts.put(t, 0);
        for (DefectMarkerType t : types) counts.put(t, counts.get(t) + 1);

        if (counts.get(DefectMarkerType.CRITICAL) > 0) {
            dominantColor = DefectMarkerType.CRITICAL.getClusterColor();
        } else if (counts.get(DefectMarkerType.HIGH) > 0) {
            dominantColor = DefectMarkerType.HIGH.getClusterColor();
        } else if (counts.get(DefectMarkerType.MEDIUM) > 0) {
            dominantColor = DefectMarkerType.MEDIUM.getClusterColor();
        } else {
            dominantColor = DefectMarkerType.LOW.getClusterColor();
        }

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(viewSizePx, viewSizePx);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = viewSizePx / 2f;
        float cy = viewSizePx / 2f;
        float radius = viewSizePx / 2f - 4f;

        bgPaint.setColor(dominantColor);
        canvas.drawCircle(cx, cy, radius, bgPaint);
        canvas.drawCircle(cx, cy, radius, borderPaint);

        // Текст: количество дефектов в кластере
        String text = clusterSize > 99 ? "99+" : String.valueOf(clusterSize);
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, cx, textY, textPaint);
    }
}