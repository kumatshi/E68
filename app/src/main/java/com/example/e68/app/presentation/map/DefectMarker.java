package com.example.e68.app.presentation.map;

import com.example.e68.app.domain.entity.Defect;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class DefectMarker implements ClusterItem {
    private final Defect defect;

    public DefectMarker(Defect defect) { this.defect = defect; }

    @Override public LatLng getPosition() { return new LatLng(defect.getLatitude(), defect.getLongitude()); }
    @Override public String getTitle() { return defect.getTitle(); }
    @Override public String getSnippet() { return defect.getStatus(); }
    @Override public Float getZIndex() { return 0f; }

    public long getDefectId() { return defect.getId(); }
    public String getStatus() { return defect.getStatus(); }
}
