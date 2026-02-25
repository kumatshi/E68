package com.example.e68.app.data.local.db;

import androidx.room.*;
import com.example.e68.app.data.local.entity.SyncQueueEntity;
import java.util.List;

@Dao
public interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    List<SyncQueueEntity> getAllPending();

    @Insert
    long insert(SyncQueueEntity item);

    @Update
    void update(SyncQueueEntity item);

    @Query("DELETE FROM sync_queue WHERE localUuid = :uuid")
    void deleteByUuid(String uuid);

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void deleteById(long id);

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, " +
            "lastError = :error WHERE id = :id")
    void incrementRetry(long id, String error);
}