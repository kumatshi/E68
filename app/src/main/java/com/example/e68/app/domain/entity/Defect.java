package com.example.e68.app.domain.entity;

public class Defect {

    private long id;
    private String title;
    private String description;
    private String type;
    private String severity;
    private String status;
    private String address;
    private double latitude;
    private double longitude;
    private String photoPath;
    private long createdAt;
    private String createdBy;

    public Defect() {
    }

    // Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // Вспомогательные методы
    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                .format(new java.util.Date(createdAt));
    }

    public int getStatusColor() {
        switch (status) {
            case "OPEN": return android.graphics.Color.parseColor("#F44336"); // Красный
            case "IN_PROGRESS": return android.graphics.Color.parseColor("#FF9800"); // Оранжевый
            case "RESOLVED": return android.graphics.Color.parseColor("#4CAF50"); // Зеленый
            case "REJECTED": return android.graphics.Color.parseColor("#9E9E9E"); // Серый
            default: return android.graphics.Color.parseColor("#000000");
        }
    }
}