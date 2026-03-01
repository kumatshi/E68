package com.example.e68.app.presentation.photo;

import android.location.Location;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.e68.app.data.remote.geocoder.GeocoderResponse;
import com.example.e68.app.data.repository.GeocoderRepository;
import com.example.e68.app.domain.usecase.CreateDefectUseCase;
import com.example.e68.app.util.Resource;

import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PhotoReportViewModel extends ViewModel {

    private final CreateDefectUseCase createDefectUseCase;
    private final GeocoderRepository  geocoderRepository;

    private final MutableLiveData<Uri>      photoUri  = new MutableLiveData<>();
    private final MutableLiveData<Location> location  = new MutableLiveData<>();
    private final MutableLiveData<String>   address   = new MutableLiveData<>();
    private final MutableLiveData<String>   category  = new MutableLiveData<>();
    private final MutableLiveData<Boolean>  isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean>  success   = new MutableLiveData<>(false);
    private final MutableLiveData<String>   error     = new MutableLiveData<>();

    @Inject
    public PhotoReportViewModel(CreateDefectUseCase createDefectUseCase,
                                GeocoderRepository geocoderRepository) {
        this.createDefectUseCase = createDefectUseCase;
        this.geocoderRepository  = geocoderRepository;
    }

    // ── Getters ───────────────────────────────────────────────────

    public LiveData<Uri>      getPhotoUri()  { return photoUri; }
    public LiveData<Location> getLocation()  { return location; }
    public LiveData<String>   getAddress()   { return address; }
    public LiveData<String>   getCategory()  { return category; }
    public LiveData<Boolean>  getIsLoading() { return isLoading; }
    public LiveData<Boolean>  getSuccess()   { return success; }
    public LiveData<String>   getError()     { return error; }

    // ── Setters ───────────────────────────────────────────────────

    public void setPhotoUri(Uri uri)    { photoUri.setValue(uri); }
    public void setCategory(String cat) { category.setValue(cat); }
    public void clearPhoto()            { photoUri.setValue(null); }

    public void setLocation(Location loc) {
        location.setValue(loc);
        address.setValue(null);

        LiveData<GeocoderResponse> liveGeo =
                geocoderRepository.reverseGeocode(loc.getLatitude(), loc.getLongitude());

        liveGeo.observeForever(new Observer<GeocoderResponse>() {
            @Override
            public void onChanged(GeocoderResponse response) {
                liveGeo.removeObserver(this);
                if (response == null) return;
                // GeocoderResponse имеет готовый хелпер getFirstAddress()
                String addr = response.getFirstAddress();
                if (addr != null) address.postValue(addr);
            }
        });
    }

    // ── Submit ────────────────────────────────────────────────────

    public void submitReport() {
        Uri      uri = photoUri.getValue();
        Location loc = location.getValue();
        String   cat = category.getValue();

        if (uri == null || loc == null || cat == null) {
            error.setValue("Заполните все поля");
            return;
        }

        isLoading.setValue(true);

        // Строим domain.entity.Defect напрямую — именно этот тип принимает usecase
        com.example.e68.app.domain.entity.Defect domainDefect =
                new com.example.e68.app.domain.entity.Defect();
        domainDefect.setLocalUuid(UUID.randomUUID().toString());
        domainDefect.setTitle(cat);                          // title = категория дефекта
        domainDefect.setType(cat);                           // type   = категория
        domainDefect.setSeverity(categoryToSeverity(cat));
        domainDefect.setStatus("OPEN");
        domainDefect.setDescription("Зафиксировано через Фото-отчёт");
        domainDefect.setLatitude(loc.getLatitude());
        domainDefect.setLongitude(loc.getLongitude());
        domainDefect.setAddress(address.getValue() != null ? address.getValue() : "");
        domainDefect.setPhotoPath(uri.toString());
        domainDefect.setCreatedAt(System.currentTimeMillis());

        LiveData<Resource<com.example.e68.app.domain.entity.Defect>> liveResult =
                createDefectUseCase.execute(domainDefect);

        liveResult.observeForever(new Observer<Resource<com.example.e68.app.domain.entity.Defect>>() {
            @Override
            public void onChanged(Resource<com.example.e68.app.domain.entity.Defect> resource) {
                if (resource == null) return;

                // Resource использует публичное поле status (не геттер)
                if (resource.status == Resource.Status.LOADING) {
                    // прогресс уже показан
                    return;
                }

                liveResult.removeObserver(this);

                if (resource.status == Resource.Status.SUCCESS) {
                    isLoading.postValue(false);
                    success.postValue(true);
                } else {
                    // Resource.Status.ERROR — поле message (не геттер)
                    isLoading.postValue(false);
                    error.postValue(resource.message != null
                            ? resource.message
                            : "Ошибка сохранения");
                }
            }
        });
    }

    // ── Reset ─────────────────────────────────────────────────────

    public void reset() {
        photoUri.setValue(null);
        location.setValue(null);
        address.setValue(null);
        category.setValue(null);
        success.setValue(false);
        error.setValue(null);
        isLoading.setValue(false);
    }

    // ── Mapping data → domain ─────────────────────────────────────


    // ── Helpers ───────────────────────────────────────────────────

    private String categoryToSeverity(String cat) {
        switch (cat) {
            case "Яма":
            case "Провал":          return "CRITICAL";
            case "Трещина":
            case "Колея":           return "HIGH";
            case "Выбоина":
            case "Разрушение края": return "MEDIUM";
            default:                return "LOW";
        }
    }

}
