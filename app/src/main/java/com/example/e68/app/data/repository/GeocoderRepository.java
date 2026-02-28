package com.example.e68.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.e68.app.data.remote.geocoder.GeocoderResponse;
import com.example.e68.app.data.remote.geocoder.YandexGeocoderService;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Репозиторий для работы с Яндекс Геокодером.
 * Внедряется через Hilt.
 */
@Singleton
public class GeocoderRepository {

    private final YandexGeocoderService service;

    @Inject
    public GeocoderRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(YandexGeocoderService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.service = retrofit.create(YandexGeocoderService.class);
    }

    /**
     * Прямой геокодинг: строка адреса → координаты.
     * Возвращает LiveData с первым результатом или null при ошибке.
     */
    public LiveData<GeocoderResponse> geocodeAddress(String address) {
        MutableLiveData<GeocoderResponse> result = new MutableLiveData<>();
        service.geocodeAddress(
                YandexGeocoderService.API_KEY,
                address,
                "json",
                1
        ).enqueue(new Callback<GeocoderResponse>() {
            @Override
            public void onResponse(Call<GeocoderResponse> call, Response<GeocoderResponse> response) {
                result.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<GeocoderResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        return result;
    }

    /**
     * Обратный геокодинг: координаты → строка адреса.
     * @param lat широта
     * @param lng долгота
     */
    public LiveData<GeocoderResponse> reverseGeocode(double lat, double lng) {
        MutableLiveData<GeocoderResponse> result = new MutableLiveData<>();
        // Яндекс принимает "longitude,latitude"
        String geoPoint = lng + "," + lat;
        service.reverseGeocode(
                YandexGeocoderService.API_KEY,
                geoPoint,
                "json",
                1
        ).enqueue(new Callback<GeocoderResponse>() {
            @Override
            public void onResponse(Call<GeocoderResponse> call, Response<GeocoderResponse> response) {
                result.postValue(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(Call<GeocoderResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        return result;
    }
}