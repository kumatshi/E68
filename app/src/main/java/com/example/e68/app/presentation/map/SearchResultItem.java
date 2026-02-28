package com.example.e68.app.presentation.map;

import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.search.BusinessObjectMetadata;
import com.yandex.mapkit.search.ToponymObjectMetadata;

/**
 * Один результат поиска на карте.
 */
public class SearchResultItem {

    public final Point point;
    public final GeoObject geoObject;

    public SearchResultItem(Point point, GeoObject geoObject) {
        this.point = point;
        this.geoObject = geoObject;
    }

    /** Отображаемое название объекта */
    public String getTitle() {
        if (geoObject == null) return "Без названия";
        String name = geoObject.getName();
        return (name != null && !name.isEmpty()) ? name : "Без названия";
    }

    /** Подзаголовок — адрес топонима или категория бизнеса */
    public String getSubtitle() {
        if (geoObject == null) return "";

        // Топоним — берём форматированный адрес
        ToponymObjectMetadata toponym = geoObject.getMetadataContainer()
                .getItem(ToponymObjectMetadata.class);
        if (toponym != null) {
            return toponym.getAddress().getFormattedAddress();
        }

        // Бизнес — берём первую категорию
        BusinessObjectMetadata business = geoObject.getMetadataContainer()
                .getItem(BusinessObjectMetadata.class);
        if (business != null && !business.getCategories().isEmpty()) {
            return business.getCategories().get(0).getName();
        }

        return "";
    }
}