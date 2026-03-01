package com.example.e68.app.presentation.photo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentPhotoReportBinding;
import com.example.e68.app.presentation.common.BaseFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PhotoReportFragment extends BaseFragment<FragmentPhotoReportBinding> {

    private PhotoReportViewModel viewModel;
    private FusedLocationProviderClient fusedLocation;

    // URI сохранённого фото в MediaStore
    private Uri pendingPhotoUri;

    // ── Launchers ─────────────────────────────────────────────────

    /** Запрос разрешений: камера + геолокация */
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean loc    = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (camera) launchCamera();
                if (loc)    fetchLocation();
            });

    /** Камера — возвращает true если фото сделано */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && pendingPhotoUri != null) {
                    viewModel.setPhotoUri(pendingPhotoUri);
                } else {
                    pendingPhotoUri = null;
                }
            });

    // ── Inflate ───────────────────────────────────────────────────

    @Override
    protected FragmentPhotoReportBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                        @Nullable ViewGroup container) {
        return FragmentPhotoReportBinding.inflate(inflater, container, false);
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel    = new ViewModelProvider(this).get(PhotoReportViewModel.class);
        fusedLocation = LocationServices.getFusedLocationProviderClient(requireContext());

        setupCategoryChips();
        setupButtons();
        observeViewModel();

        // Автоматически получаем геолокацию при открытии вкладки
        autoFetchLocation();
    }

    // ── Setup ─────────────────────────────────────────────────────

    private void setupCategoryChips() {
        // Чипы уже в XML — просто слушаем выбор
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setCategory(null);
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) viewModel.setCategory(chip.getText().toString());
        });
    }

    private void setupButtons() {

        // ── Кнопка "Сделать фото" ─────────────────────────────────
        binding.btnTakePhoto.setOnClickListener(v -> {
            if (hasCameraPermission()) {
                launchCamera();
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
        });

        // ── Кнопка обновления GPS ─────────────────────────────────
        binding.btnRefreshGps.setOnClickListener(v -> fetchLocation());

        // ── Кнопка "Отправить отчёт" ──────────────────────────────
        binding.btnSendReport.setOnClickListener(v -> {
            if (validate()) {
                viewModel.submitReport();
            }
        });

        // ── Сброс (повторная съёмка) ──────────────────────────────
        binding.btnRetakePhoto.setOnClickListener(v -> {
            viewModel.clearPhoto();
            pendingPhotoUri = null;
        });
    }

    // ── Camera ────────────────────────────────────────────────────

    private void launchCamera() {
        // Создаём запись в MediaStore чтобы получить Uri до съёмки
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "defect_" + System.currentTimeMillis() + ".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        pendingPhotoUri = requireContext().getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        if (pendingPhotoUri != null) {
            cameraLauncher.launch(pendingPhotoUri);
        } else {
            showToast("Не удалось создать файл фото");
        }
    }

    // ── Location ──────────────────────────────────────────────────

    private void autoFetchLocation() {
        if (hasLocationPermission()) {
            fetchLocation();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchLocation() {
        if (!hasLocationPermission()) return;

        binding.tvGpsStatus.setText("Определяем местоположение…");
        binding.progressGps.setVisibility(View.VISIBLE);

        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    binding.progressGps.setVisibility(View.GONE);
                    if (location != null) {
                        viewModel.setLocation(location);
                    } else {
                        // Fallback — последнее известное
                        fetchLastKnownLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressGps.setVisibility(View.GONE);
                    binding.tvGpsStatus.setText("GPS недоступен");
                });
    }

    @SuppressLint("MissingPermission")
    private void fetchLastKnownLocation() {
        fusedLocation.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        viewModel.setLocation(location);
                    } else {
                        binding.tvGpsStatus.setText("GPS недоступен");
                    }
                });
    }

    // ── Validation ────────────────────────────────────────────────

    private boolean validate() {
        boolean ok = true;

        if (viewModel.getPhotoUri().getValue() == null) {
            showToast("Сначала сделайте фото дефекта");
            ok = false;
        }

        if (viewModel.getCategory().getValue() == null) {
            binding.tvCategoryError.setVisibility(View.VISIBLE);
            ok = false;
        } else {
            binding.tvCategoryError.setVisibility(View.GONE);
        }

        if (viewModel.getLocation().getValue() == null) {
            showToast("Дождитесь определения геолокации");
            ok = false;
        }

        return ok;
    }

    // ── Observers ─────────────────────────────────────────────────

    private void observeViewModel() {

        // Фото
        viewModel.getPhotoUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                binding.ivPhotoPreview.setImageURI(uri);
                binding.ivPhotoPreview.setVisibility(View.VISIBLE);
                binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                binding.btnRetakePhoto.setVisibility(View.VISIBLE);
                binding.btnTakePhoto.setText("Переснять");
            } else {
                binding.ivPhotoPreview.setVisibility(View.GONE);
                binding.layoutPhotoPlaceholder.setVisibility(View.VISIBLE);
                binding.btnRetakePhoto.setVisibility(View.GONE);
                binding.btnTakePhoto.setText("📷  Сделать фото");
            }
        });

        // Геолокация
        viewModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            if (location != null) {
                String coords = String.format("%.6f, %.6f  (±%.0fm)",
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getAccuracy());
                binding.tvGpsStatus.setText(coords);
                binding.ivGpsIcon.setImageResource(R.drawable.ic_location_on);
            }
        });

        // Адрес (обратное геокодирование из ViewModel)
        viewModel.getAddress().observe(getViewLifecycleOwner(), address -> {
            if (address != null && !address.isEmpty()) {
                binding.tvAddress.setText(address);
                binding.tvAddress.setVisibility(View.VISIBLE);
            }
        });

        // Загрузка / отправка
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnSendReport.setEnabled(!loading);
            binding.progressSend.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSendReport.setText(loading ? "Отправляем…" : "Отправить отчёт");
        });

        // Успех
        viewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                showToast("Дефект зафиксирован!");
                viewModel.reset();
                binding.chipGroupCategory.clearCheck();
            }
        });

        // Ошибка
        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) showToast(err);
        });
    }

    // ── Permissions ───────────────────────────────────────────────

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}