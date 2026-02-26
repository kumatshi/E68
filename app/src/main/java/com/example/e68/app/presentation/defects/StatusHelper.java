package com.example.e68.app.presentation.defects;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import android.graphics.Color;

public class StatusHelper {

    public static void applyStatus(TextView badge, String status) {
        String label;
        int textColor;
        int bgColor;

        switch (status != null ? status : "") {
            case "OPEN":
                label = "Открыт"; textColor = Color.parseColor("#FF4757"); bgColor = Color.parseColor("#33130A20");
                break;
            case "IN_PROGRESS":
                label = "В работе"; textColor = Color.parseColor("#FFD166"); bgColor = Color.parseColor("#33332B0A");
                break;
            case "RESOLVED":
                label = "Устранён"; textColor = Color.parseColor("#06D6A0"); bgColor = Color.parseColor("#330A2B23");
                break;
            case "REJECTED":
                label = "Отклонён"; textColor = Color.parseColor("#8892A4"); bgColor = Color.parseColor("#221A1F2E");
                break;
            default:
                label = status != null ? status : "—"; textColor = Color.WHITE; bgColor = Color.TRANSPARENT;
        }

        badge.setText(label);
        badge.setTextColor(textColor);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(20f);
        bg.setColor(bgColor);
        bg.setStroke(2, textColor);
        badge.setBackground(bg);
    }
}
