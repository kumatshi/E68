package com.example.e68.app.presentation.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentMapBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
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
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MapFragment extends BaseFragment<FragmentMapBinding> {

    private static final double CLUSTER_RADIUS   = 60.0;
    // Integer.MAX_VALUE — кластеры видны на любом уровне приближения
    private static final int    CLUSTER_MIN_ZOOM = Integer.MAX_VALUE;

    private MapViewModel viewModel;

    // ── Коллекции маркеров ────────────────────────────────────────
    private ClusterizedPlacemarkCollection defectClusterCollection;
    private MapObjectCollection            searchResultCollection;

    // GC guard
    private final List<MapObjectTapListener> tapListeners = new ArrayList<>();

    // ── Адаптер саджестов ─────────────────────────────────────────
    private SuggestAdapter suggestAdapter;

    // ── Геолокация ────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private PlacemarkMapObject userLocationMarker;
    private LocationCallback   locationCallback;
    private boolean            isTrackingLocation = false;
    private ValueAnimator      trackingDotAnimator;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            moveToMyLocation();
                        } else {
                            showToast("Разрешение на геолокацию не выдано");
                        }
                    });

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupMap();
        setupSearch();
        setupFilterChips();
        setupMyLocationButton();
        observeDefects();
        observeSelectedDefect();
        observeSearch();
    }

    @Override public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        binding.mapView.onStart();

        // Возобновить отслеживание, если было активно до ухода в фон
        if (isTrackingLocation) {
            isTrackingLocation = false; // сбросим флаг, startLocationTracking проверит
            startLocationTracking();
        }
    }

    @Override public void onStop() {
        // Останавливаем обновления (экономия батареи), но флаг isTrackingLocation
        // оставляем true — чтобы onStart() знал, что надо возобновить
        if (isTrackingLocation && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
            // isTrackingLocation намеренно НЕ сбрасываем
        }
        binding.mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override public void onDestroyView() {
        // Полная остановка + очистка маркера
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        isTrackingLocation = false;

        if (trackingDotAnimator != null) {
            trackingDotAnimator.cancel();
            trackingDotAnimator = null;
        }

        userLocationMarker = null; // коллекция удалится вместе с mapView
        tapListeners.clear();
        super.onDestroyView();
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

    // ── My Location ───────────────────────────────────────────────

    private void setupMyLocationButton() {
        binding.btnMyLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                moveToMyLocation();
            } else {
                locationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });

        binding.btnStopTracking.setOnClickListener(v -> stopLocationTracking());
    }

    private void moveToMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        showToast("Не удалось определить местоположение");
                        return;
                    }
                    Point myPoint = new Point(location.getLatitude(), location.getLongitude());

                    // Центрируем камеру (как раньше)
                    binding.mapView.getMap().move(
                            new CameraPosition(myPoint, 16f, 0f, 0f),
                            new Animation(Animation.Type.SMOOTH, 0.5f),
                            null);

                    // Создаём/обновляем маркер сразу при нажатии
                    updateUserLocationMarker(myPoint);

                    // Запускаем реалтайм-отслеживание (если ещё не запущено)
                    if (!isTrackingLocation) {
                        startLocationTracking();
                    }
                })
                .addOnFailureListener(e ->
                        showToast("Ошибка геолокации: " + e.getMessage()));
    }

// ── Маркер пользователя ───────────────────────────────────────

    /**
     * Создаёт маркер при первом вызове, затем только обновляет координаты.
     * Не трогает defectClusterCollection и searchResultCollection.
     */
    private void updateUserLocationMarker(Point point) {
        if (userLocationMarker == null) {
            // Создаём маркер в отдельной коллекции (поверх всего)
            MapObjectCollection userLocationCollection = binding.mapView.getMap()
                    .getMapObjects()
                    .addCollection();

            userLocationMarker = userLocationCollection.addPlacemark();
            userLocationMarker.setIcon(
                    ImageProvider.fromResource(requireContext(), R.drawable.user_location_marker),
                    new IconStyle()
                            .setAnchor(new PointF(0.5f, 0.5f))
                            .setScale(1.2f)
                            .setZIndex(200f));
        }
        userLocationMarker.setGeometry(point);
    }

// ── Отслеживание в реальном времени ──────────────────────────

    private void startLocationTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        if (isTrackingLocation) return; // уже запущено

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000L) // интервал 2 сек
                .setMinUpdateIntervalMillis(1000L)       // минимум 1 сек
                .setMinUpdateDistanceMeters(1f)          // минимум 1 метр
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                android.location.Location loc = result.getLastLocation();
                if (loc == null) return;
                Point newPoint = new Point(loc.getLatitude(), loc.getLongitude());
                updateUserLocationMarker(newPoint);
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper());

        isTrackingLocation = true;
        showTrackingIndicator();
    }

    private void stopLocationTracking() {
        if (!isTrackingLocation || locationCallback == null) return;

        fusedLocationClient.removeLocationUpdates(locationCallback);
        locationCallback    = null;
        isTrackingLocation  = false;

        hideTrackingIndicator();
    }

// ── UI: индикатор отслеживания ────────────────────────────────

    private void showTrackingIndicator() {
        binding.cardTrackingIndicator.setVisibility(View.VISIBLE);
        binding.btnStopTracking.setVisibility(View.VISIBLE);

        // Пульсация точки (alpha 1.0 → 0.3 → 1.0, бесконечно)
        trackingDotAnimator = ObjectAnimator.ofFloat(
                binding.viewTrackingDot, "alpha", 1f, 0.3f);
        trackingDotAnimator.setDuration(900);
        trackingDotAnimator.setRepeatMode(ValueAnimator.REVERSE);
        trackingDotAnimator.setRepeatCount(ValueAnimator.INFINITE);
        trackingDotAnimator.setInterpolator(new LinearInterpolator());
        trackingDotAnimator.start();
    }

    private void hideTrackingIndicator() {
        binding.cardTrackingIndicator.setVisibility(View.GONE);
        binding.btnStopTracking.setVisibility(View.GONE);

        if (trackingDotAnimator != null) {
            trackingDotAnimator.cancel();
            trackingDotAnimator = null;
        }
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

        container.addView(makeDivider(dp1, 0xFF_DDDDDD));

        for (Defect defect : defects) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp12, 0, dp12);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(getRowRipple());

            TextView tvTitle = new TextView(requireContext());
            tvTitle.setText(defect.getTitle() != null ? defect.getTitle() : "Без названия");
            tvTitle.setTextSize(14f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            row.addView(tvTitle);

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

    // ── Bottom Sheet — дефект ─────────────────────────────────────

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