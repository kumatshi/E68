package com.example.e68.app.data.remote.geocoder;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit-интерфейс для Яндекс Геокодера.
 * Docs: https://yandex.ru/dev/geocode/doc/ru/
 */
public interface YandexGeocoderService {

    String BASE_URL = "https://geocode-maps.yandex.ru/";
    String API_KEY  = "bdef66ce-fa38-4992-a8e9-358f85526525";

    /**
     * Прямой геокодинг: адрес → координаты
     * @param geocode  строка адреса (напр. "Москва, Арбат 10")
     */
    @GET("1.x/")
    Call<GeocoderResponse> geocodeAddress(
            @Query("apikey")  String apiKey,
            @Query("geocode") String geocode,
            @Query("format")  String format,   // "json"
            @Query("results") int    results    // кол-во результатов
    );

    /**
     * Обратный геокодинг: координаты → адрес
     * @param geocode  "longitude,latitude" (именно в таком порядке!)
     */
    @GET("1.x/")
    Call<GeocoderResponse> reverseGeocode(
            @Query("apikey")  String apiKey,
            @Query("geocode") String geocode,
            @Query("format")  String format,
            @Query("results") int    results
    );
}