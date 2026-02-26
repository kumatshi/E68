package com.example.e68.app.presentation.defects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.CreateDefectUseCase;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@HiltViewModel
public class DefectsViewModel extends BaseViewModel {

    private final GetAllDefectsUseCase getAllDefectsUseCase;
    private final CreateDefectUseCase createDefectUseCase;

    private final MutableLiveData<String> _filter = new MutableLiveData<>("ALL");
    private final LiveData<List<Defect>> _allDefects;
    private final MediatorLiveData<List<Defect>> _filteredDefects = new MediatorLiveData<>();

    @Inject
    public DefectsViewModel(GetAllDefectsUseCase getAllDefects, CreateDefectUseCase createDefect) {
        this.getAllDefectsUseCase = getAllDefects;
        this.createDefectUseCase = createDefect;
        _allDefects = getAllDefectsUseCase.execute();

        // Merge: re-filter when either data or filter changes
        _filteredDefects.addSource(_allDefects, defects -> applyFilter());
        _filteredDefects.addSource(_filter, f -> applyFilter());
    }

    private void applyFilter() {
        List<Defect> all = _allDefects.getValue();
        String filter = _filter.getValue();
        if (all == null) { _filteredDefects.setValue(new ArrayList<>()); return; }
        if ("ALL".equals(filter)) { _filteredDefects.setValue(all); return; }
        _filteredDefects.setValue(all.stream()
                .filter(d -> filter.equals(d.getStatus()))
                .collect(Collectors.toList()));
    }

    public LiveData<List<Defect>> getAllDefects() { return _allDefects; }
    public LiveData<List<Defect>> getFilteredDefects() { return _filteredDefects; }

    public void setFilter(String filter) {
        _filter.setValue(filter);
    }
}
