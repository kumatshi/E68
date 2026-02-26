package com.example.e68.app.presentation.patrol;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.e68.app.databinding.FragmentPatrolBinding;
import com.example.e68.app.presentation.common.BaseFragment;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@AndroidEntryPoint
public class PatrolFragment extends BaseFragment<FragmentPatrolBinding> {

    private PatrolViewModel viewModel;
    private MapObjectCollection mapObjects;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long elapsedSeconds = 0;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            elapsedSeconds++;
            long h = TimeUnit.SECONDS.toHours(elapsedSeconds);
            long m = TimeUnit.SECONDS.toMinutes(elapsedSeconds) % 60;
            long s = elapsedSeconds % 60;
            if (binding != null)
                binding.durationValue.setText(
                        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected FragmentPatrolBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                   @Nullable ViewGroup container) {
        return FragmentPatrolBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PatrolViewModel.class);

        // Начальная позиция карты
        binding.patrolMapView.getMap().move(
                new CameraPosition(new Point(55.7558, 37.6173), 12f, 0f, 0f)
        );
        mapObjects = binding.patrolMapView.getMap().getMapObjects();

        setupButtons();
        observeViewModel();
    }

    private void setupButtons() {
        binding.btnStartPatrol.setOnClickListener(v -> {
            String name = binding.routeNameEditText.getText() != null
                    ? binding.routeNameEditText.getText().toString().trim() : "";
            if (name.isEmpty())
                name = "Маршрут " + java.text.SimpleDateFormat
                        .getDateInstance().format(new java.util.Date());
            viewModel.startPatrol(name);
        });

        binding.btnStopPatrol.setOnClickListener(v -> viewModel.stopPatrol());
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
                binding.distanceValue.setText(
                        String.format(Locale.getDefault(), "%.0f", route.getDistance()));
                binding.defectsFoundValue.setText(
                        String.valueOf(route.getDefectsFound()));
            }
        });

        viewModel.getRoutePoints().observe(getViewLifecycleOwner(), points -> {
            if (points == null || points.size() < 2) return;

            // Строим полилинию маршрута
            List<Point> yandexPoints = new ArrayList<>();
            for (com.example.e68.app.domain.entity.GpsPoint p : points) {
                yandexPoints.add(new Point(p.getLatitude(), p.getLongitude()));
            }

            mapObjects.clear();
            PolylineMapObject polyline = mapObjects.addPolyline(new Polyline(yandexPoints));
            polyline.setStrokeColor(0xFFFF6B35); // оранжевый
            polyline.setStrokeWidth(5f);

            // Следим за последней точкой
            Point last = yandexPoints.get(yandexPoints.size() - 1);
            binding.patrolMapView.getMap().move(
                    new CameraPosition(last, 16f, 0f, 0f)
            );
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        binding.patrolMapView.onStart();
    }

    @Override
    public void onStop() {
        binding.patrolMapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}