package com.example.e68.app.domain.usecase;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.repository.DefectRepository;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GetAllDefectsUseCase {

    private final DefectRepository defectRepository;

    @Inject
    public GetAllDefectsUseCase(DefectRepository defectRepository) {
        this.defectRepository = defectRepository;
    }

    public LiveData<List<Defect>> execute() {
        return defectRepository.getAllDefects();
    }
}