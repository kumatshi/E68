package com.example.e68.app.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class PatrolRoute {

    private long id;
    private String name;
    private long startTime;
    private Long endTime;
    private double distance; // в метрах
    private List<GpsPoint> points = new ArrayList<>();
    private int defectsFound;

    public PatrolRoute() {
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public Long getEndTime() { return endTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public List<GpsPoint> getPoints() { return points; }
    public void setPoints(List<GpsPoint> points) { this.points = points; }

    public int getDefectsFound() { return defectsFound; }
    public void setDefectsFound(int defectsFound) { this.defectsFound = defectsFound; }

    public boolean isActive() {
        return endTime == null;
    }

    public String getDuration() {
        if (endTime == null) return "Активен";
        long duration = endTime - startTime;
        long hours = duration / 3600000;
        long minutes = (duration % 3600000) / 60000;
        return String.format("%02d:%02d", hours, minutes);
    }
}