package com.example.e68.app.data.local.entity;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;

@Entity(tableName = "defects",
        indices = {@Index("serverId"), @Index("status"), @Index("inspectorId")})
public class DefectEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @Nullable
    public Long serverId;

    public String localUuid;
    public double latitude;
    public double longitude;
    public String defectType;
    public String severity;
    public String status;
    public String description;
    public int priority;
    public String photoPath;

    @Nullable
    public String photoUrl;

    public String inspectorId;

    @Nullable
    public String assignedTo;

    public long createdAt;
    public long updatedAt;
    public boolean isSynced;
    public boolean isPendingDelete;

    // Конструктор по умолчанию
    public DefectEntity() {}
}