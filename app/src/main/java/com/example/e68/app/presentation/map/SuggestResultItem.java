package com.example.e68.app.presentation.map;

/**
 * Один элемент подсказки поиска.
 */
public class SuggestResultItem {

    public final String title;
    public final String subtitle;   // может быть null
    public final String searchText; // текст для подстановки в поле
    public final String uri;        // URI для прямого поиска (может быть null)
    public final boolean isSearchAction; // true → сразу запустить поиск

    public SuggestResultItem(String title, String subtitle,
                             String searchText, String uri,
                             boolean isSearchAction) {
        this.title = title;
        this.subtitle = subtitle;
        this.searchText = searchText;
        this.uri = uri;
        this.isSearchAction = isSearchAction;
    }
}