package com.example.e68.app.presentation.analytics;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.data.report.ReportGenerator;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AnalyticsViewModel extends AndroidViewModel {

    private final GetAllDefectsUseCase getAllDefectsUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Результат генерации: null = ошибка, непустая строка = путь к файлу
    private final MutableLiveData<String> _pdfResult   = new MutableLiveData<>();
    private final MutableLiveData<String> _excelResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _loading    = new MutableLiveData<>(false);

    public LiveData<String>  getPdfResult()   { return _pdfResult; }
    public LiveData<String>  getExcelResult() { return _excelResult; }
    public LiveData<Boolean> getLoading()     { return _loading; }

    @Inject
    public AnalyticsViewModel(Application app, GetAllDefectsUseCase getAllDefectsUseCase) {
        super(app);
        this.getAllDefectsUseCase = getAllDefectsUseCase;
    }

    public LiveData<List<Defect>> getDefects() {
        return getAllDefectsUseCase.execute();
    }

    public void generatePdf(List<Defect> defects) {
        _loading.setValue(true);
        executor.execute(() -> {
            String path = new ReportGenerator(getApplication()).generatePdf(defects);
            _loading.postValue(false);
            _pdfResult.postValue(path);   // null если ошибка
        });
    }

    public void generateExcel(List<Defect> defects) {
        _loading.setValue(true);
        executor.execute(() -> {
            String path = new ReportGenerator(getApplication()).generateExcel(defects);
            _loading.postValue(false);
            _excelResult.postValue(path);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}