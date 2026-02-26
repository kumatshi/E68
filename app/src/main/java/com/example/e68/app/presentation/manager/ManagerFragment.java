// FILE: app/src/main/java/com/example/e68/app/presentation/manager/ManagerFragment.java
// ЗАМЕНИ ПОЛНОСТЬЮ
package com.example.e68.app.presentation.manager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentManagerMapBinding;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Стартовый экран менеджера — карта со всеми дефектами.
 * ID этого фрагмента совпадает с nav_manager_map в меню менеджера.
 */
@AndroidEntryPoint
public class ManagerFragment extends Fragment {

    private FragmentManagerMapBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManagerMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMap();
    }

    private void setupMap() {
        try {
            binding.mapView.getMap().move(
                    new CameraPosition(new Point(55.751574, 37.573856), 11f, 0f, 0f)
            );
        } catch (Exception ignored) {}
    }

    @Override
    public void onStart() {
        super.onStart();
        try { MapKitFactory.getInstance().onStart(); binding.mapView.onStart(); } catch (Exception ignored) {}
    }

    @Override
    public void onStop() {
        try { binding.mapView.onStop(); MapKitFactory.getInstance().onStop(); } catch (Exception ignored) {}
        super.onStop();
    }
}