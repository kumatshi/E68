package com.example.e68.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue")
public class SyncQueueEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String entityType;
    public String operation;
    public String localUuid;
    public String payload;
    public int retryCount;
    public long createdAt;
    public String lastError;

    public SyncQueueEntity() {}
}