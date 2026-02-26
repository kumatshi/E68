package com.example.e68.app.presentation.patrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.e68.app.databinding.FragmentPatrolBinding;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import dagger.hilt.android.AndroidEntryPoint;
import android.graphics.Color;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class PatrolFragment extends BaseFragment<FragmentPatrolBinding> implements OnMapReadyCallback {

    private PatrolViewModel viewModel;
    private GoogleMap googleMap;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedSeconds = 0;

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && googleMap != null) enableMapLocation();
            });

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            elapsedSeconds++;
            long h = TimeUnit.SECONDS.toHours(elapsedSeconds);
            long m = TimeUnit.SECONDS.toMinutes(elapsedSeconds) % 60;
            long s = elapsedSeconds % 60;
            if (binding != null)
                binding.durationValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected FragmentPatrolBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPatrolBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PatrolViewModel.class);

        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(com.example.e68.app.R.id.patrolMapFragment);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        setupButtons();
        observeViewModel();
    }

    private void setupButtons() {
        binding.btnStartPatrol.setOnClickListener(v -> {
            String name = binding.routeNameEditText.getText() != null ?
                    binding.routeNameEditText.getText().toString().trim() : "";
            if (name.isEmpty()) name = "Маршрут " + java.text.SimpleDateFormat.getDateInstance().format(new java.util.Date());
            viewModel.startPatrol(name);
        });

        binding.btnStopPatrol.setOnClickListener(v -> {
            viewModel.stopPatrol();
        });
    }

    private void observeViewModel() {
        viewModel.isPatrolActive().observe(getViewLifecycleOwner(), active -> {
            binding.startPanel.setVisibility(active ? View.GONE : View.VISIBLE);
            binding.activePanel.setVisibility(active ? View.VISIBLE : View.GONE);
            binding.activePatrolOverlay.setVisibility(active ? View.VISIBLE : View.GONE);

            if (active) {
                elapsedSeconds = 0;
                timerHandler.post(timerRunnable);
            } else {
                timerHandler.removeCallbacks(timerRunnable);
            }
        });

        viewModel.getActiveRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                binding.routeNameLabel.setText(route.getName());
                binding.distanceValue.setText(String.format(Locale.getDefault(), "%.0f", route.getDistance()));
                binding.defectsFoundValue.setText(String.valueOf(route.getDefectsFound()));
            }
        });

        viewModel.getRoutePoints().observe(getViewLifecycleOwner(), points -> {
            if (googleMap == null || points == null || points.size() < 2) return;
            // Draw the track polyline
            PolylineOptions opts = new PolylineOptions()
                    .color(Color.parseColor("#FF6B35"))
                    .width(8f)
                    .geodesic(true);
            for (com.example.e68.app.domain.entity.GpsPoint p : points) {
                opts.add(new LatLng(p.getLatitude(), p.getLongitude()));
            }
            googleMap.clear();
            googleMap.addPolyline(opts);

            // Follow last point
            com.example.e68.app.domain.entity.GpsPoint last = points.get(points.size() - 1);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(last.getLatitude(), last.getLongitude()), 16f));
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        // Dark map style
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                    com.example.e68.app.R.raw.map_style_dark));
        } catch (Exception ignored) {}

        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMapLocation();
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableMapLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
