package com.example.e68.app.data.local.db;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.e68.app.data.local.entity.DefectEntity;
import java.util.List;

@Dao
public interface DefectDao {

    @Query("SELECT * FROM defects WHERE isPendingDelete = 0 ORDER BY createdAt DESC")
    LiveData<List<DefectEntity>> getAllDefects();

    @Query("SELECT * FROM defects WHERE latitude BETWEEN :minLat AND :maxLat " +
            "AND longitude BETWEEN :minLng AND :maxLng AND isPendingDelete = 0")
    LiveData<List<DefectEntity>> getDefectsInBounds(double minLat, double maxLat,
                                                    double minLng, double maxLng);

    @Query("SELECT * FROM defects WHERE isSynced = 0 AND isPendingDelete = 0")
    List<DefectEntity> getUnsynced();

    @Query("SELECT * FROM defects WHERE status = :status")
    LiveData<List<DefectEntity>> getByStatus(String status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DefectEntity defect);

    @Update
    void update(DefectEntity defect);

    @Query("UPDATE defects SET serverId=:serverId, isSynced=1, photoUrl=:photoUrl " +
            "WHERE localUuid=:uuid")
    void markSynced(String uuid, long serverId, String photoUrl);

    @Query("DELETE FROM defects WHERE id = :id")
    void deleteById(long id);
}