package com.example.e68.app.data.remote.geocoder;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Модели для парсинга ответа Яндекс Геокодера.
 */
public class GeocoderResponse {

    @SerializedName("response")
    public Response response;

    public static class Response {
        @SerializedName("GeoObjectCollection")
        public GeoObjectCollection geoObjectCollection;
    }

    public static class GeoObjectCollection {
        @SerializedName("featureMember")
        public List<FeatureMember> featureMember;
    }

    public static class FeatureMember {
        @SerializedName("GeoObject")
        public GeoObject geoObject;
    }

    public static class GeoObject {
        /** Форматированный адрес */
        @SerializedName("metaDataProperty")
        public MetaDataProperty metaDataProperty;

        /** Координаты: "longitude latitude" */
        @SerializedName("Point")
        public Point point;
    }

    public static class MetaDataProperty {
        @SerializedName("GeocoderMetaData")
        public GeocoderMetaData geocoderMetaData;
    }

    public static class GeocoderMetaData {
        /** Полный форматированный адрес */
        @SerializedName("text")
        public String text;

        @SerializedName("Address")
        public Address address;
    }

    public static class Address {
        @SerializedName("formatted")
        public String formatted;
    }

    public static class Point {
        /** Строка вида "37.617698 55.755864" (lon lat) */
        @SerializedName("pos")
        public String pos;

        /** Парсит долготу из строки pos */
        public double getLongitude() {
            if (pos == null) return 0;
            String[] parts = pos.split(" ");
            return parts.length >= 1 ? Double.parseDouble(parts[0]) : 0;
        }

        /** Парсит широту из строки pos */
        public double getLatitude() {
            if (pos == null) return 0;
            String[] parts = pos.split(" ");
            return parts.length >= 2 ? Double.parseDouble(parts[1]) : 0;
        }
    }

    // ─── Хелперы ───────────────────────────────────────────────────────────────

    /** Возвращает первый GeoObject или null */
    public GeoObject getFirstResult() {
        if (response == null
                || response.geoObjectCollection == null
                || response.geoObjectCollection.featureMember == null
                || response.geoObjectCollection.featureMember.isEmpty()) {
            return null;
        }
        return response.geoObjectCollection.featureMember.get(0).geoObject;
    }

    /** Удобный метод: первый форматированный адрес или null */
    public String getFirstAddress() {
        GeoObject obj = getFirstResult();
        if (obj == null || obj.metaDataProperty == null
                || obj.metaDataProperty.geocoderMetaData == null) return null;
        return obj.metaDataProperty.geocoderMetaData.text;
    }

    /** Удобный метод: координаты первого результата или null */
    public Point getFirstPoint() {
        GeoObject obj = getFirstResult();
        return (obj == null) ? null : obj.point;
    }
}