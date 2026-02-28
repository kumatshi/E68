package com.example.e68.app.presentation.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.domain.usecase.GetAllDefectsUseCase;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.VisibleRegion;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MapViewModel extends ViewModel {

    // ── Дефекты ───────────────────────────────────────────────────

    private final GetAllDefectsUseCase getAllDefectsUseCase;

    private final MutableLiveData<List<Defect>> allDefects = new MutableLiveData<>();
    private final MutableLiveData<String> severityFilter = new MutableLiveData<>(null);
    private final MutableLiveData<Defect> selectedDefect = new MutableLiveData<>();

    // ── Поиск ─────────────────────────────────────────────────────

    private final MapSearchManager searchManager = new MapSearchManager();

    private final MutableLiveData<List<SearchResultItem>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<List<SuggestResultItem>> suggests = new MutableLiveData<>();
    private final MutableLiveData<BoundingBox> searchBoundingBox = new MutableLiveData<>();
    private final MutableLiveData<Boolean> searchLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> searchError = new MutableLiveData<>();
    private final MutableLiveData<String> queryText = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> searchActive = new MutableLiveData<>(false);

    private VisibleRegion currentRegion;

    @Inject
    public MapViewModel(GetAllDefectsUseCase getAllDefectsUseCase) {
        this.getAllDefectsUseCase = getAllDefectsUseCase;
        loadDefects();
    }

    // ── Дефекты ───────────────────────────────────────────────────

    private void loadDefects() {
        getAllDefectsUseCase.execute().observeForever(defects -> allDefects.setValue(defects));
    }

    public LiveData<List<Defect>> getAllDefects() { return allDefects; }

    public LiveData<String> getSeverityFilter() { return severityFilter; }

    public void setSeverityFilter(String filter) { severityFilter.setValue(filter); }

    public LiveData<Defect> getSelectedDefect() { return selectedDefect; }

    public void selectDefect(Defect defect) { selectedDefect.setValue(defect); }

    // ── Карта / регион ────────────────────────────────────────────

    public void setVisibleRegion(VisibleRegion region) {
        this.currentRegion = region;
    }

    // ── Поиск ─────────────────────────────────────────────────────

    public LiveData<List<SearchResultItem>> getSearchResults() { return searchResults; }
    public LiveData<List<SuggestResultItem>> getSuggests() { return suggests; }
    public LiveData<BoundingBox> getSearchBoundingBox() { return searchBoundingBox; }
    public LiveData<Boolean> getSearchLoading() { return searchLoading; }
    public LiveData<String> getSearchError() { return searchError; }
    public LiveData<String> getQueryText() { return queryText; }
    public LiveData<Boolean> getSearchActive() { return searchActive; }

    public void setQueryText(String text) {
        queryText.setValue(text);
        if (text == null || text.isEmpty()) {
            suggests.setValue(null);
            searchManager.resetSuggest();
            return;
        }
        // Запускаем саджест
        if (currentRegion != null) {
            BoundingBox box = MapSearchManager.regionToBoundingBox(currentRegion);
            searchManager.suggest(text, box, new MapSearchManager.SuggestCallback() {
                @Override
                public void onSuggests(List<SuggestResultItem> items) {
                    suggests.postValue(items);
                }

                @Override
                public void onError(String message) {
                    // тихая ошибка для саджеста
                }
            });
        }
    }

    /** Запуск поиска по текущему тексту */
    public void startSearch() {
        String query = queryText.getValue();
        if (query == null || query.isEmpty() || currentRegion == null) return;
        executeSearch(query);
    }

    /** Поиск по конкретному тексту (из саджеста) */
    public void startSearchByText(String text) {
        queryText.setValue(text);
        suggests.setValue(null);
        if (currentRegion == null) return;
        executeSearch(text);
    }

    private void executeSearch(String query) {
        searchLoading.setValue(true);
        searchActive.setValue(true);
        suggests.setValue(null);
        searchManager.cancelSearch();
        searchManager.search(query, currentRegion, new MapSearchManager.SearchCallback() {
            @Override
            public void onResults(List<SearchResultItem> items, BoundingBox boundingBox) {
                searchLoading.postValue(false);
                searchResults.postValue(items);
                if (boundingBox != null) {
                    searchBoundingBox.postValue(boundingBox);
                }
            }

            @Override
            public void onError(String message) {
                searchLoading.postValue(false);
                searchError.postValue(message);
            }
        });
    }

    /** Сброс поиска — возврат к режиму дефектов */
    public void resetSearch() {
        searchManager.cancelSearch();
        searchManager.resetSuggest();
        searchResults.setValue(null);
        suggests.setValue(null);
        queryText.setValue("");
        searchActive.setValue(false);
        searchLoading.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        searchManager.cancelSearch();
    }
}