package com.example.e68.app.presentation.defects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.CreateDefectUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import com.example.e68.app.util.Resource;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CreateDefectViewModel extends BaseViewModel {

    private final CreateDefectUseCase createDefectUseCase;
    private String defectTypeCode = null;
    private String severity = "MEDIUM";

    private final MutableLiveData<Resource<Defect>> _createResult = new MutableLiveData<>();
    public LiveData<Resource<Defect>> getCreateResult() { return _createResult; }

    @Inject
    public CreateDefectViewModel(CreateDefectUseCase useCase) {
        this.createDefectUseCase = useCase;
    }

    public void setDefectTypeCode(String code) { this.defectTypeCode = code; }
    public String getDefectTypeCode() { return defectTypeCode; }

    public void setSeverity(String severity) { this.severity = severity; }
    public String getSeverity() { return severity; }

    public void createDefect(Defect defect) {
        setLoading(true);
        defect.setSeverity(severity);
        createDefectUseCase.execute(defect).observeForever(result -> {
            setLoading(false);
            _createResult.postValue(result);
        });
    }
}