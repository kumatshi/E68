package com.example.e68.app.presentation.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import com.google.android.gms.maps.model.LatLngBounds;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@HiltViewModel
public class MapViewModel extends BaseViewModel {

    private final GetAllDefectsUseCase getAllDefectsUseCase;
    private final MutableLiveData<LatLngBounds> _bounds = new MutableLiveData<>();

    private final LiveData<List<Defect>> _allDefects;
    private final MutableLiveData<List<Defect>> _defects = new MutableLiveData<>(Collections.emptyList());

    @Inject
    public MapViewModel(GetAllDefectsUseCase getAllDefectsUseCase) {
        this.getAllDefectsUseCase = getAllDefectsUseCase;
        _allDefects = getAllDefectsUseCase.execute();
        _allDefects.observeForever(defects -> filterByBounds(defects, _bounds.getValue()));
        _bounds.observeForever(bounds -> filterByBounds(_allDefects.getValue(), bounds));
    }

    private void filterByBounds(List<Defect> all, LatLngBounds bounds) {
        if (all == null) { _defects.postValue(Collections.emptyList()); return; }
        if (bounds == null) { _defects.postValue(all); return; }
        List<Defect> filtered = all.stream()
                .filter(d -> bounds.contains(new com.google.android.gms.maps.model.LatLng(d.getLatitude(), d.getLongitude())))
                .collect(Collectors.toList());
        _defects.postValue(filtered);
    }

    public LiveData<List<Defect>> getDefects() { return _defects; }

    public void onBoundsChanged(LatLngBounds bounds) {
        _bounds.postValue(bounds);
    }
}
