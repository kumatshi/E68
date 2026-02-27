package com.example.e68.app.domain.usecase;

import androidx.lifecycle.LiveData;

import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.util.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UpdateDefectUseCase {

    private final DefectRepository defectRepository;

    @Inject
    public UpdateDefectUseCase(DefectRepository defectRepository) {
        this.defectRepository = defectRepository;
    }

    public LiveData<Resource<Defect>> execute(Defect defect) {
        return defectRepository.updateDefect(defect);
    }
}