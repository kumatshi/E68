package com.example.e68.app.presentation.map;

import com.example.e68.app.R;

/**
 * Тип маркера дефекта — определяет цвет пина по severity.
 */
public enum DefectMarkerType {
    LOW,       // зелёный
    MEDIUM,    // жёлтый
    HIGH,      // оранжевый
    CRITICAL;  // красный

    /** Иконка пина для каждого типа */
    public int getPinDrawable() {
        switch (this) {
            case LOW:      return R.drawable.pin_low;
            case MEDIUM:   return R.drawable.pin_medium;
            case HIGH:     return R.drawable.pin_high;
            case CRITICAL: return R.drawable.pin_critical;
            default:       return R.drawable.pin_medium;
        }
    }

    /** Цвет для кластера */
    public int getClusterColor() {
        switch (this) {
            case LOW:      return 0xFF06D6A0; // зелёный
            case MEDIUM:   return 0xFFFFD166; // жёлтый
            case HIGH:     return 0xFFFF6B35; // оранжевый
            case CRITICAL: return 0xFFFF4757; // красный
            default:       return 0xFFFFD166;
        }
    }

    /** Парсит из строки severity */
    public static DefectMarkerType fromSeverity(String severity) {
        if (severity == null) return MEDIUM;
        switch (severity.toUpperCase()) {
            case "LOW":      return LOW;
            case "HIGH":     return HIGH;
            case "CRITICAL": return CRITICAL;
            default:         return MEDIUM;
        }
    }
}