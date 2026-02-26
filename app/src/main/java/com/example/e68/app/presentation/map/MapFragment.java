package com.example.e68.app.presentation.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentMapBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class MapFragment extends BaseFragment<FragmentMapBinding> implements OnMapReadyCallback {

    private MapViewModel viewModel;
    private GoogleMap googleMap;
    private ClusterManager<DefectMarker> clusterManager;

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && googleMap != null) enableMyLocation();
            });

    @Override
    protected FragmentMapBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentMapBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.googleMap);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        // FAB create defect
        binding.fabAddDefect.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_defectsList_to_createDefect));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Apply dark style
        try {
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark));
        } catch (Exception ignored) {}

        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setCompassEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(55.7558, 37.6173), 12f));

        setupClustering();
        observeDefects();

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void setupClustering() {
        clusterManager = new ClusterManager<>(requireContext(), googleMap);
        googleMap.setOnCameraIdleListener(() -> {
            clusterManager.onCameraIdle();
            viewModel.onBoundsChanged(googleMap.getProjection().getVisibleRegion().latLngBounds);
        });
        googleMap.setOnMarkerClickListener(clusterManager);
        clusterManager.setOnClusterItemClickListener(item -> {
            // Navigate to defect detail
            MapFragmentDirections.ActionMapToDefectDetail action =
                    MapFragmentDirections.actionMapToDefectDetail(item.getDefectId());
            Navigation.findNavController(requireView()).navigate(action);
            return true;
        });
    }

    private void observeDefects() {
        viewModel.getDefects().observe(getViewLifecycleOwner(), defects -> {
            if (clusterManager != null && defects != null) {
                clusterManager.clearItems();
                for (Defect d : defects) {
                    clusterManager.addItem(new DefectMarker(d));
                }
                clusterManager.cluster();
            }
        });
    }

    private void enableMyLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
        } catch (Exception ignored) {}
    }

    private float getHueForStatus(String status) {
        switch (status != null ? status : "") {
            case "OPEN":        return BitmapDescriptorFactory.HUE_RED;
            case "IN_PROGRESS": return BitmapDescriptorFactory.HUE_ORANGE;
            case "RESOLVED":    return BitmapDescriptorFactory.HUE_GREEN;
            case "REJECTED":    return BitmapDescriptorFactory.HUE_AZURE;
            default:            return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }
}
