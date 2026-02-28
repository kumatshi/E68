package com.example.e68.app.presentation.map;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.VisibleRegion;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.SuggestItem;
import com.yandex.mapkit.search.SuggestOptions;
import com.yandex.mapkit.search.SuggestResponse;
import com.yandex.mapkit.search.SuggestSession;
import com.yandex.mapkit.search.SuggestType;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

/**
 * Обёртка над Яндекс Search API.
 * Предоставляет поиск по тексту и саджест подсказок.
 */
public class MapSearchManager {

    public interface SearchCallback {
        void onResults(List<SearchResultItem> items, BoundingBox boundingBox);
        void onError(String message);
    }

    public interface SuggestCallback {
        void onSuggests(List<SuggestResultItem> items);
        void onError(String message);
    }

    private static final int SUGGEST_LIMIT = 10;
    private static final SuggestOptions SUGGEST_OPTIONS = new SuggestOptions()
            .setSuggestTypes(
                    SuggestType.GEO.value |
                            SuggestType.BIZ.value |
                            SuggestType.TRANSIT.value
            );

    private final SearchManager searchManager;
    private final SuggestSession suggestSession;
    private Session activeSearchSession;

    public MapSearchManager() {
        searchManager = SearchFactory.getInstance()
                .createSearchManager(SearchManagerType.COMBINED);
        suggestSession = searchManager.createSuggestSession();
    }

    // ── Search ────────────────────────────────────────────────────

    public void search(String query, VisibleRegion region, SearchCallback callback) {
        if (query == null || query.isEmpty() || region == null) return;

        cancelSearch();

        Geometry geometry = VisibleRegionUtils.toPolygon(region);


        SearchOptions options = new SearchOptions();
        options.setResultPageSize(32);

        activeSearchSession = searchManager.submit(
                query,
                geometry,
                options,
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(Response response) {
                        List<SearchResultItem> items = new ArrayList<>();
                        for (com.yandex.mapkit.GeoObjectCollection.Item child :
                                response.getCollection().getChildren()) {
                            if (child.getObj() == null) continue;
                            if (child.getObj().getGeometry().isEmpty()) continue;
                            Point point = child.getObj().getGeometry().get(0).getPoint();
                            if (point == null) continue;
                            items.add(new SearchResultItem(point, child.getObj()));
                        }
                        BoundingBox bbox = response.getMetadata().getBoundingBox();
                        if (bbox != null) {
                            callback.onResults(items, bbox);
                        } else if (!items.isEmpty()) {
                            callback.onResults(items, null);
                        }
                    }

                    @Override
                    public void onSearchError(Error error) {
                        callback.onError("Ошибка поиска");
                    }
                }
        );
    }

    public void cancelSearch() {
        if (activeSearchSession != null) {
            activeSearchSession.cancel();
            activeSearchSession = null;
        }
    }

    // ── Suggest ───────────────────────────────────────────────────

    public void suggest(String query, BoundingBox box, SuggestCallback callback) {
        if (query == null || query.isEmpty() || box == null) {
            callback.onSuggests(new ArrayList<>());
            return;
        }

        suggestSession.suggest(query, box, SUGGEST_OPTIONS,
                new SuggestSession.SuggestListener() {
                    @Override
                    public void onResponse(SuggestResponse response) {
                        List<SuggestResultItem> items = new ArrayList<>();
                        int count = Math.min(response.getItems().size(), SUGGEST_LIMIT);
                        for (int i = 0; i < count; i++) {
                            SuggestItem item = response.getItems().get(i);
                            String title = item.getTitle() != null
                                    ? item.getTitle().getText() : "";
                            String subtitle = item.getSubtitle() != null
                                    ? item.getSubtitle().getText() : null;
                            String searchText = item.getSearchText();
                            String uri = item.getUri();
                            boolean isSearch = item.getAction() == SuggestItem.Action.SEARCH;
                            items.add(new SuggestResultItem(title, subtitle, searchText, uri, isSearch));
                        }
                        callback.onSuggests(items);
                    }

                    @Override
                    public void onError(Error error) {
                        callback.onError("Ошибка подсказок");
                    }
                });
    }

    public void resetSuggest() {
        suggestSession.reset();
    }

    // ── Utility ───────────────────────────────────────────────────

    public static BoundingBox regionToBoundingBox(VisibleRegion region) {
        return new BoundingBox(region.getBottomLeft(), region.getTopRight());
    }
}