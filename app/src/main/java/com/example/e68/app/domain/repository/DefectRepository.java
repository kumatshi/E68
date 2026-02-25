package com.example.e68.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.util.Resource;
import java.util.List;

public interface DefectRepository {

    LiveData<List<Defect>> getAllDefects();

    LiveData<Defect> getDefectById(long id);

    LiveData<Resource<Defect>> createDefect(Defect defect);

    LiveData<Resource<Defect>> updateDefect(Defect defect);

    LiveData<Resource<Void>> deleteDefect(long id);

    LiveData<List<Defect>> getDefectsByStatus(String status);

    List<Defect> getUnsyncedDefects();
}