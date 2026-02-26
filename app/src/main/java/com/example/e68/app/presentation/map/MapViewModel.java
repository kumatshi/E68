package com.example.e68.app.presentation.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel для карты дефектов.
 * Google Maps удалён. Для Yandex Maps используйте onBoundsChanged(double, double, double, double).
 */
@HiltViewModel
public class MapViewModel extends BaseViewModel {

    private final GetAllDefectsUseCase getAllDefectsUseCase;
    private final MutableLiveData<List<Defect>> _defects = new MutableLiveData<>(Collections.emptyList());

    @Inject
    public MapViewModel(GetAllDefectsUseCase getAllDefectsUseCase) {
        this.getAllDefectsUseCase = getAllDefectsUseCase;
        getAllDefectsUseCase.execute().observeForever(defects -> {
            if (defects != null) _defects.postValue(defects);
        });
    }

    public LiveData<List<Defect>> getDefects() {
        return _defects;
    }

    /**
     * Вызывайте при изменении видимой области Яндекс Карты.
     * Параметры — координаты углов видимой области.
     */
    public void onBoundsChanged(double minLat, double minLon, double maxLat, double maxLon) {
        List<Defect> all = getAllDefectsUseCase.execute().getValue();
        if (all == null) { _defects.postValue(Collections.emptyList()); return; }
        List<Defect> filtered = new java.util.ArrayList<>();
        for (Defect d : all) {
            if (d.getLatitude() >= minLat && d.getLatitude() <= maxLat
                    && d.getLongitude() >= minLon && d.getLongitude() <= maxLon) {
                filtered.add(d);
            }
        }
        _defects.postValue(filtered);
    }
}
