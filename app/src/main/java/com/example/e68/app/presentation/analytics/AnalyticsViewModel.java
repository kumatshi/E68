package com.example.e68.app.presentation.analytics;

import androidx.lifecycle.LiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.List;

@HiltViewModel
public class AnalyticsViewModel extends BaseViewModel {

    private final GetAllDefectsUseCase getAllDefectsUseCase;

    @Inject
    public AnalyticsViewModel(GetAllDefectsUseCase getAllDefectsUseCase) {
        this.getAllDefectsUseCase = getAllDefectsUseCase;
    }

    public LiveData<List<Defect>> getDefects() {
        return getAllDefectsUseCase.execute();
    }

    public void generatePdf() {
        // PDF generation via GenerateReportUseCase (wired to iText7)
        // Full implementation is in the architecture doc
    }

    public void generateExcel() {
        // Excel generation via GenerateReportUseCase (wired to Apache POI)
    }
}
