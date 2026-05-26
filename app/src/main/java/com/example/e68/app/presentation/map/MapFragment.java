package com.example.e68.app.presentation.map;

import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentMapBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.ClusterListener;
import com.yandex.mapkit.map.ClusterTapListener;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.ui_view.ViewProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MapFragment extends BaseFragment<FragmentMapBinding> {

    private static final double CLUSTER_RADIUS   = 60.0;
    private static final int    CLUSTER_MIN_ZOOM = 20; // увеличено — кластеры не пропадают

    private MapViewModel viewModel;

    // ── Коллекции маркеров ────────────────────────────────────────
    private ClusterizedPlacemarkCollection defectClusterCollection;
    private MapObjectCollection            searchResultCollection;

    // GC guard
    private final List<MapObjectTapListener> tapListeners = new ArrayList<>();

    // ── Адаптер саджестов ─────────────────────────────────────────
    private SuggestAdapter suggestAdapter;

    // ── Camera listener ───────────────────────────────────────────
    private final CameraListener cameraListener = (map, pos, reason, finished) -> {
        if (reason == CameraUpdateReason.GESTURES && finished) {
            viewModel.setVisibleRegion(binding.mapView.getMap().getVisibleRegion());
        }
    };

    // ── Cluster listeners ─────────────────────────────────────────

    private final ClusterTapListener clusterTapListener = cluster -> {
        List<Defect> defectsInCluster = new ArrayList<>();
        for (PlacemarkMapObject p : cluster.getPlacemarks()) {
            Object ud = p.getUserData();
            if (ud instanceof DefectMarker) {
                defectsInCluster.add(((DefectMarker) ud).defect);
            }
        }
        if (defectsInCluster.size() == 1) {
            navigateToDefectDetail(defectsInCluster.get(0));
        } else {
            showClusterSelectionSheet(defectsInCluster);
        }
        return true;
    };

    private final ClusterListener clusterListener = cluster -> {
        List<DefectMarkerType> types = new ArrayList<>();
        for (PlacemarkMapObject p : cluster.getPlacemarks()) {
            Object ud = p.getUserData();
            if (ud instanceof DefectMarker) types.add(((DefectMarker) ud).markerType);
        }
        DefectClusterView view = new DefectClusterView(requireContext());
        view.setData(types);
        cluster.getAppearance().setView(new ViewProvider(view));
        cluster.getAppearance().setZIndex(100f);
        cluster.addClusterTapListener(clusterTapListener);
    };

    // ── Inflate ───────────────────────────────────────────────────

    @Override
    protected FragmentMapBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container) {
        return FragmentMapBinding.inflate(inflater, container, false);
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        setupMap();
        setupSearch();
        setupFilterChips();
        observeDefects();
        observeSelectedDefect();
        observeSearch();
    }

    @Override public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        binding.mapView.onStart();
    }

    @Override public void onStop() {
        binding.mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        tapListeners.clear();
    }

    // ── Map setup ─────────────────────────────────────────────────

    private void setupMap() {
        binding.mapView.getMap().move(
                new CameraPosition(new Point(51.7727, 55.0988), 12f, 0f, 0f));

        binding.mapView.getMap().addCameraListener(cameraListener);
        viewModel.setVisibleRegion(binding.mapView.getMap().getVisibleRegion());

        defectClusterCollection = binding.mapView.getMap()
                .getMapObjects()
                .addClusterizedPlacemarkCollection(clusterListener);

        searchResultCollection = binding.mapView.getMap()
                .getMapObjects()
                .addCollection();
    }

    // ── Search setup ──────────────────────────────────────────────

    private void setupSearch() {
        suggestAdapter = new SuggestAdapter(item -> {
            hideSuggests();
            hideKeyboard();
            if (item.isSearchAction || item.uri != null) {
                viewModel.startSearchByText(item.searchText);
            } else {
                binding.etSearch.setText(item.searchText);
                binding.etSearch.setSelection(item.searchText.length());
            }
        });

        binding.rvSuggests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggests.setAdapter(suggestAdapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                viewModel.setQueryText(text);
                binding.btnSearchClear.setVisibility(
                        text.isEmpty() ? View.GONE : View.VISIBLE);
                if (text.isEmpty()) {
                    hideSuggests();
                    viewModel.resetSearch();
                }
            }
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                hideSuggests();
                hideKeyboard();
                viewModel.startSearch();
                return true;
            }
            return false;
        });

        binding.btnSearch.setOnClickListener(v -> {
            hideSuggests();
            hideKeyboard();
            viewModel.startSearch();
        });

        binding.btnSearchClear.setOnClickListener(v -> {
            binding.etSearch.setText("");
            hideSuggests();
            hideKeyboard();
            viewModel.resetSearch();
            binding.btnSearchClear.setVisibility(View.GONE);
            binding.chipGroupFilter.setVisibility(View.VISIBLE);
        });
    }

    // ── Filter chips ──────────────────────────────────────────────

    private void setupFilterChips() {
        binding.chipAll.setOnCheckedChangeListener((b, c)      -> { if (c) viewModel.setSeverityFilter(null); });
        binding.chipLow.setOnCheckedChangeListener((b, c)      -> { if (c) viewModel.setSeverityFilter("LOW"); });
        binding.chipMedium.setOnCheckedChangeListener((b, c)   -> { if (c) viewModel.setSeverityFilter("MEDIUM"); });
        binding.chipHigh.setOnCheckedChangeListener((b, c)     -> { if (c) viewModel.setSeverityFilter("HIGH"); });
        binding.chipCritical.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverityFilter("CRITICAL"); });
        binding.chipAll.setChecked(true);
    }

    // ── Observers — дефекты ───────────────────────────────────────

    private void observeDefects() {
        viewModel.getAllDefects().observe(getViewLifecycleOwner(), defects -> {
            if (Boolean.TRUE.equals(viewModel.getSearchActive().getValue())) return;
            rebuildDefectMarkers(defects, viewModel.getSeverityFilter().getValue());
        });

        viewModel.getSeverityFilter().observe(getViewLifecycleOwner(), filter -> {
            if (Boolean.TRUE.equals(viewModel.getSearchActive().getValue())) return;
            List<Defect> defects = viewModel.getAllDefects().getValue();
            if (defects != null) rebuildDefectMarkers(defects, filter);
        });
    }

    private void observeSelectedDefect() {
        viewModel.getSelectedDefect().observe(getViewLifecycleOwner(), defect -> {
            if (defect != null) showDefectBottomSheet(defect);
        });
    }

    // ── Observers — поиск ─────────────────────────────────────────

    private void observeSearch() {
        viewModel.getSuggests().observe(getViewLifecycleOwner(), items -> {
            if (items == null || items.isEmpty()) {
                hideSuggests();
            } else {
                suggestAdapter.submitList(items);
                binding.rvSuggests.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), items -> {
            if (items == null) {
                clearSearchMarkers();
                return;
            }
            showSearchResults(items);
            binding.chipGroupFilter.setVisibility(View.GONE);
        });

        viewModel.getSearchBoundingBox().observe(getViewLifecycleOwner(), bbox -> {
            if (bbox != null) zoomToBoundingBox(bbox);
        });

        viewModel.getSearchLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressSearch.setVisibility(
                        Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getSearchError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) showToast(err);
        });

        viewModel.getSearchActive().observe(getViewLifecycleOwner(), active -> {
            if (!Boolean.TRUE.equals(active)) {
                clearSearchMarkers();
                binding.chipGroupFilter.setVisibility(View.VISIBLE);
                List<Defect> defects = viewModel.getAllDefects().getValue();
                if (defects != null) {
                    rebuildDefectMarkers(defects, viewModel.getSeverityFilter().getValue());
                }
            }
        });
    }

    // ── Маркеры дефектов ──────────────────────────────────────────

    private void rebuildDefectMarkers(List<Defect> all, @Nullable String filter) {
        defectClusterCollection.clear();
        tapListeners.clear();

        for (Defect defect : all) {
            if (defect.getLatitude() == 0 && defect.getLongitude() == 0) continue;
            if (filter != null && !filter.equals(defect.getSeverity())) continue;

            DefectMarkerType type = DefectMarkerType.fromSeverity(defect.getSeverity());
            PlacemarkMapObject pm = defectClusterCollection.addPlacemark();
            pm.setGeometry(new Point(defect.getLatitude(), defect.getLongitude()));
            pm.setIcon(ImageProvider.fromResource(requireContext(), type.getPinDrawable()),
                    new IconStyle().setAnchor(new PointF(0.5f, 1.0f)).setScale(0.7f));
            pm.setUserData(new DefectMarker(defect, type));

            MapObjectTapListener listener = (obj, pt) -> {
                Object ud = obj.getUserData();
                if (ud instanceof DefectMarker) {
                    navigateToDefectDetail(((DefectMarker) ud).defect);
                }
                return true;
            };
            tapListeners.add(listener);
            pm.addTapListener(listener);
        }
        defectClusterCollection.clusterPlacemarks(CLUSTER_RADIUS, CLUSTER_MIN_ZOOM);
    }

    // ── Маркеры поиска ────────────────────────────────────────────

    private void showSearchResults(List<SearchResultItem> items) {
        searchResultCollection.clear();
        tapListeners.clear();

        ImageProvider searchIcon = ImageProvider.fromResource(
                requireContext(), R.drawable.ic_search_pin);

        for (SearchResultItem item : items) {
            PlacemarkMapObject pm = searchResultCollection.addPlacemark();
            pm.setGeometry(item.point);
            pm.setIcon(searchIcon, new IconStyle()
                    .setAnchor(new PointF(0.5f, 1.0f))
                    .setScale(0.6f));
            pm.setUserData(item);

            MapObjectTapListener listener = (obj, pt) -> {
                Object ud = obj.getUserData();
                if (ud instanceof SearchResultItem) showSearchBottomSheet((SearchResultItem) ud);
                return true;
            };
            tapListeners.add(listener);
            pm.addTapListener(listener);
        }
    }

    private void clearSearchMarkers() {
        searchResultCollection.clear();
    }

    // ── Навигация в detail ────────────────────────────────────────

    private void navigateToDefectDetail(Defect defect) {
        Bundle args = new Bundle();
        args.putLong("defectId", defect.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_mapFragment_to_defectDetail, args);
    }

    // ── Bottom sheet: выбор дефекта из кластера ───────────────────

    private void showClusterSelectionSheet(List<Defect> defects) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        int dp16 = dp(16);
        int dp12 = dp(12);
        int dp8  = dp(8);
        int dp4  = dp(4);
        int dp1  = dp(1);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp16, dp16, dp16, dp16);

        // Заголовок
        TextView header = new TextView(requireContext());
        header.setText("Дефекты в кластере (" + defects.size() + ")");
        header.setTextSize(16f);
        header.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = dp12;
        header.setLayoutParams(headerParams);
        container.addView(header);

        // Разделитель под заголовком
        container.addView(makeDivider(dp1, 0xFF_DDDDDD));

        for (Defect defect : defects) {
            // Строка дефекта
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp12, 0, dp12);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(getRowRipple());

            // Название
            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(defect.getTitle() != null ? defect.getTitle() : "Без названия");
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            row.addView(tvTitle);

            // Адрес
            TextView tvAddress = new TextView(requireContext());
            tvAddress.setText(defect.getAddress() != null ? defect.getAddress() : "Адрес не указан");
            tvAddress.setTextSize(12f);
            tvAddress.setAlpha(0.6f);
            LinearLayout.LayoutParams addrParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            addrParams.topMargin = dp4 / 2;
            tvAddress.setLayoutParams(addrParams);
            row.addView(tvAddress);

            // Severity badge
            String severity = defect.getSeverity() != null ? defect.getSeverity() : "MEDIUM";
            TextView tvSeverity = new TextView(requireContext());
            tvSeverity.setText(severity);
            tvSeverity.setTextSize(11f);
            tvSeverity.setTextColor(Color.WHITE);
            tvSeverity.setTypeface(null, Typeface.BOLD);
            tvSeverity.setPadding(dp8, dp4, dp8, dp4);
            tvSeverity.setBackgroundColor(
                    DefectMarkerType.fromSeverity(severity).getClusterColor());
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.topMargin = dp4;
            tvSeverity.setLayoutParams(badgeParams);
            row.addView(tvSeverity);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                navigateToDefectDetail(defect);
            });

            container.addView(row);
            container.addView(makeDivider(dp1, 0xFF_EEEEEE));
        }

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(container);

        dialog.setContentView(scrollView);
        dialog.show();
    }

    // ── Bottom Sheet — дефект (одиночный маркер) ──────────────────

    private void showDefectBottomSheet(Defect defect) {
        hideSearchBottomSheet();
        binding.cardDefectPreview.setVisibility(View.VISIBLE);

        binding.tvDefectTitle.setText(
                defect.getTitle() != null ? defect.getTitle() : "Без названия");
        binding.tvDefectAddress.setText(
                defect.getAddress() != null ? defect.getAddress() : "Адрес не указан");

        String severity = defect.getSeverity() != null ? defect.getSeverity() : "MEDIUM";
        binding.tvDefectSeverity.setText(severity);
        binding.tvDefectSeverity.setBackgroundColor(
                DefectMarkerType.fromSeverity(severity).getClusterColor());
        binding.tvDefectStatus.setText(
                defect.getStatus() != null ? defect.getStatus() : "—");

        binding.btnDefectDetails.setOnClickListener(v -> {
            binding.cardDefectPreview.setVisibility(View.GONE);
            navigateToDefectDetail(defect);
        });

        binding.btnClosePreview.setOnClickListener(v ->
                binding.cardDefectPreview.setVisibility(View.GONE));
    }

    // ── Bottom Sheet — результат поиска ───────────────────────────

    private void showSearchBottomSheet(SearchResultItem item) {
        hideDefectBottomSheet();
        binding.cardSearchPreview.setVisibility(View.VISIBLE);

        binding.tvSearchResultTitle.setText(item.getTitle());
        binding.tvSearchResultSubtitle.setText(item.getSubtitle());

        binding.btnCloseSearchPreview.setOnClickListener(v -> hideSearchBottomSheet());
    }

    private void hideDefectBottomSheet() {
        binding.cardDefectPreview.setVisibility(View.GONE);
    }

    private void hideSearchBottomSheet() {
        binding.cardSearchPreview.setVisibility(View.GONE);
    }

    // ── Camera ────────────────────────────────────────────────────

    private void zoomToBoundingBox(BoundingBox bbox) {
        CameraPosition pos;
        try {
            pos = binding.mapView.getMap()
                    .cameraPosition(Geometry.fromBoundingBox(bbox));
        } catch (Exception e) {
            return;
        }
        binding.mapView.getMap().move(
                pos, new Animation(Animation.Type.SMOOTH, 0.5f), null);
    }

    // ── UI helpers ────────────────────────────────────────────────

    private void hideSuggests() {
        binding.rvSuggests.setVisibility(View.GONE);
        suggestAdapter.submitList(Collections.emptyList());
    }

    private void hideKeyboard() {
        View v = requireActivity().getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private View makeDivider(int heightPx, int color) {
        View divider = new View(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightPx);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(color);
        return divider;
    }

    private android.graphics.drawable.Drawable getRowRipple() {
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta =
                requireContext().obtainStyledAttributes(attrs);
        android.graphics.drawable.Drawable ripple = ta.getDrawable(0);
        ta.recycle();
        return ripple;
    }

    // ── Inner classes ─────────────────────────────────────────────

    static class DefectMarker {
        final Defect defect;
        final DefectMarkerType markerType;
        DefectMarker(Defect d, DefectMarkerType t) { defect = d; markerType = t; }
    }
}