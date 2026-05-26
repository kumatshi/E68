package com.example.e68.app.domain.usecase;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.repository.DefectRepository;
import com.example.e68.app.util.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CreateDefectUseCase {

    private final DefectRepository defectRepository;

    @Inject
    public CreateDefectUseCase(DefectRepository defectRepository) {
        this.defectRepository = defectRepository;
    }

    public LiveData<Resource<Defect>> execute(Defect defect) {

        if (defect.getTitle() == null || defect.getTitle().isEmpty()) {
            return new LiveData<Resource<Defect>>() {
                @Override
                protected void postValue(Resource<Defect> value) {
                    super.postValue(Resource.error("Название обязательно", null));
                }
            };
        }

        return defectRepository.createDefect(defect);
    }
}