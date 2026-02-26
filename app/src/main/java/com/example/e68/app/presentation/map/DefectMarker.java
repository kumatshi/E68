package com.example.e68.app.presentation.map;

import com.example.e68.app.domain.entity.Defect;

/**
 * Обёртка над Defect для отображения маркера на карте.
 * При интеграции Yandex Maps используйте этот класс для создания PlacemarkMapObject.
 */
public class DefectMarker {

    private final Defect defect;

    public DefectMarker(Defect defect) {
        this.defect = defect;
    }

    public double getLatitude()  { return defect.getLatitude(); }
    public double getLongitude() { return defect.getLongitude(); }
    public String getTitle()     { return defect.getTitle(); }
    public String getStatus()    { return defect.getStatus(); }
    public long   getDefectId()  { return defect.getId(); }
}
