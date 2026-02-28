package com.example.e68.app.presentation.defects;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.data.remote.geocoder.GeocoderResponse;
import com.example.e68.app.data.repository.GeocoderRepository;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.CreateDefectUseCase;
import com.example.e68.app.presentation.common.BaseViewModel;
import com.example.e68.app.util.Resource;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class CreateDefectViewModel extends BaseViewModel {

    private final CreateDefectUseCase createDefectUseCase;
    private final GeocoderRepository geocoderRepository;

    private String defectTypeCode = null;
    private String severity = "MEDIUM";

    // Результат создания дефекта
    private final MutableLiveData<Resource<Defect>> _createResult = new MutableLiveData<>();
    public LiveData<Resource<Defect>> getCreateResult() { return _createResult; }

    // Результат геокодирования (адрес → координаты)
    private final MutableLiveData<GeocoderResponse> _geocodeResult = new MutableLiveData<>();
    public LiveData<GeocoderResponse> getGeocodeResult() { return _geocodeResult; }

    // Результат обратного геокодирования (координаты → адрес)
    private final MutableLiveData<GeocoderResponse> _reverseGeocodeResult = new MutableLiveData<>();
    public LiveData<GeocoderResponse> getReverseGeocodeResult() { return _reverseGeocodeResult; }

    // Состояние загрузки геокодера
    private final MutableLiveData<Boolean> _isGeocodingLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isGeocodingLoading() { return _isGeocodingLoading; }

    @Inject
    public CreateDefectViewModel(CreateDefectUseCase useCase, GeocoderRepository geocoderRepository) {
        this.createDefectUseCase = useCase;
        this.geocoderRepository = geocoderRepository;
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

    /**
     * Прямой геокодинг: строка адреса → координаты.
     * Наблюдай за getGeocodeResult() во фрагменте.
     */
    public void geocodeAddress(String address) {
        _isGeocodingLoading.setValue(true);
        geocoderRepository.geocodeAddress(address).observeForever(response -> {
            _isGeocodingLoading.postValue(false);
            _geocodeResult.postValue(response);
        });
    }

    /**
     * Обратный геокодинг: GPS → строка адреса.
     * Наблюдай за getReverseGeocodeResult() во фрагменте.
     */
    public void reverseGeocode(double lat, double lng) {
        _isGeocodingLoading.setValue(true);
        geocoderRepository.reverseGeocode(lat, lng).observeForever(response -> {
            _isGeocodingLoading.postValue(false);
            _reverseGeocodeResult.postValue(response);
        });
    }
}