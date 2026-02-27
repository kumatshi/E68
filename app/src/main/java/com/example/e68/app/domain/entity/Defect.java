package com.example.e68.app.domain.entity;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Defect {

    private long   id;
    private String localUuid;   // Firestore document ID
    private String title;
    private String description;
    private String type;
    private String severity;
    private String status;
    private String address;
    private double latitude;
    private double longitude;
    private String photoPath;
    private long   createdAt;
    private String createdBy;

    public Defect() {}

    public long   getId()                    { return id; }
    public void   setId(long id)             { this.id = id; }

    public String getLocalUuid()             { return localUuid; }
    public void   setLocalUuid(String v)     { this.localUuid = v; }

    public String getTitle()                 { return title; }
    public void   setTitle(String v)         { this.title = v; }

    public String getDescription()           { return description; }
    public void   setDescription(String v)   { this.description = v; }

    public String getType()                  { return type; }
    public void   setType(String v)          { this.type = v; }

    public String getSeverity()              { return severity; }
    public void   setSeverity(String v)      { this.severity = v; }

    public String getStatus()                { return status; }
    public void   setStatus(String v)        { this.status = v; }

    public String getAddress()               { return address; }
    public void   setAddress(String v)       { this.address = v; }

    public double getLatitude()              { return latitude; }
    public void   setLatitude(double v)      { this.latitude = v; }

    public double getLongitude()             { return longitude; }
    public void   setLongitude(double v)     { this.longitude = v; }

    public String getPhotoPath()             { return photoPath; }
    public void   setPhotoPath(String v)     { this.photoPath = v; }

    public long   getCreatedAt()             { return createdAt; }
    public void   setCreatedAt(long v)       { this.createdAt = v; }

    public String getCreatedBy()             { return createdBy; }
    public void   setCreatedBy(String v)     { this.createdBy = v; }

    public String getFormattedDate() {
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                .format(new java.util.Date(createdAt));
    }

    public int getStatusColor() {
        if (status == null) return android.graphics.Color.parseColor("#000000");
        switch (status) {
            case "OPEN":        return android.graphics.Color.parseColor("#F44336");
            case "IN_PROGRESS": return android.graphics.Color.parseColor("#FF9800");
            case "RESOLVED":    return android.graphics.Color.parseColor("#4CAF50");
            case "REJECTED":    return android.graphics.Color.parseColor("#9E9E9E");
            default:            return android.graphics.Color.parseColor("#000000");
        }
    }
}