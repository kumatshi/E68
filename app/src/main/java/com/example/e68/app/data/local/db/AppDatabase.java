package com.example.e68.app.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.e68.app.data.local.entity.DefectEntity;
import com.example.e68.app.data.local.entity.SyncQueueEntity;

@Database(
        entities = {
                DefectEntity.class,
                SyncQueueEntity.class
        },
        version = 1,
        exportSchema = true
)
@TypeConverters({DateConverter.class, ListConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract DefectDao defectDao();
    public abstract SyncQueueDao syncQueueDao();
}