package com.example.e68.app.presentation.defects;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.e68.app.R;
import com.example.e68.app.data.remote.geocoder.GeocoderResponse;
import com.example.e68.app.databinding.FragmentCreateDefectBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.example.e68.app.util.Resource;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateDefectFragment extends BaseFragment<FragmentCreateDefectBinding> {

    private CreateDefectViewModel viewModel;
    private FusedLocationProviderClient locationClient;

    private double pickedLat = 0;
    private double pickedLng = 0;

    // Фото
    private Uri photoUri = null;
    private Uri cameraFileUri = null;

    private static final String[][] DEFECT_TYPES = {
            {"PH_001", "Выбоина"},
            {"PH_002", "Колея"},
            {"PH_003", "Трещина попер."},
            {"PH_004", "Трещина прод."},
            {"PH_005", "Просадка"},
            {"MK_001", "Люк / решётка"},
            {"MK_002", "Бордюр"},
            {"SW_001", "Светофор"},
            {"SW_002", "Дорожный знак"},
            {"DR_001", "Ливневая кан."},
    };

    // ── Launchers ────────────────────────────────────────────────

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) fetchGps();
                else showToast("Разрешение на геолокацию отклонено");
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else showToast("Разрешение на камеру отклонено");
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && cameraFileUri != null) {
                    photoUri = cameraFileUri;
                    showPhotoPreview(photoUri);
                } else {
                    showToast("Фото не было сделано");
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoUri = uri;
                    showPhotoPreview(photoUri);
                }
            });

    // ── Inflate ──────────────────────────────────────────────────

    @Override
    protected FragmentCreateDefectBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                         @Nullable ViewGroup container) {
        return FragmentCreateDefectBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateDefectViewModel.class);
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupToolbar();
        setupDefectTypeChips();
        setupSeverityChips();
        setupGpsButton();
        setupAddressGeocoder(); // ← новое
        setupPhotoButtons();
        setupSubmitButton();
        observeViewModel();
    }

    // ── Setup ─────────────────────────────────────────────────────

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupDefectTypeChips() {
        for (String[] pair : DEFECT_TYPES) {
            String code  = pair[0];
            String label = pair[1];

            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.selector_chip_bg);
            chip.setTextColor(getResources().getColorStateList(R.color.selector_chip_text, null));
            chip.setChipStrokeColorResource(R.color.selector_chip_stroke);
            chip.setChipStrokeWidth(2f);

            chip.setOnCheckedChangeListener((btn, checked) -> {
                if (checked) {
                    viewModel.setDefectTypeCode(code);
                    for (int i = 0; i < binding.defectTypeChipGroup.getChildCount(); i++) {
                        Chip other = (Chip) binding.defectTypeChipGroup.getChildAt(i);
                        if (other != btn && other.isChecked()) other.setChecked(false);
                    }
                }
            });

            binding.defectTypeChipGroup.addView(chip);
        }
    }

    private void setupSeverityChips() {
        binding.chipLow.setOnCheckedChangeListener((b, c)      -> { if (c) viewModel.setSeverity("LOW"); });
        binding.chipMedium.setOnCheckedChangeListener((b, c)   -> { if (c) viewModel.setSeverity("MEDIUM"); });
        binding.chipHigh.setOnCheckedChangeListener((b, c)     -> { if (c) viewModel.setSeverity("HIGH"); });
        binding.chipCritical.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverity("CRITICAL"); });
        binding.chipMedium.setChecked(true);
    }

    private void setupGpsButton() {
        binding.btnGetGps.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchGps();
            } else {
                locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        });
    }

    // ── Яндекс Геокодер ───────────────────────────────────────────

    private void setupAddressGeocoder() {
        // Сбрасываем ошибку и координаты когда пользователь редактирует адрес вручную
        binding.etAddress.addTextChangedListener(addressWatcher);

        // Кнопка прямого геокодинга: введённый адрес → координаты
        binding.btnFindAddress.setOnClickListener(v -> {
            String address = binding.etAddress.getText().toString().trim();
            if (TextUtils.isEmpty(address)) {
                binding.tilAddress.setError("Введите адрес для поиска");
                return;
            }
            viewModel.geocodeAddress(address);
        });
    }

    private void setupPhotoButtons() {
        binding.btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnPickPhoto.setOnClickListener(v ->
                galleryLauncher.launch("image/*"));

        binding.btnRemovePhoto.setOnClickListener(v -> {
            photoUri = null;
            cameraFileUri = null;
            binding.ivPhotoPreview.setImageURI(null);
            binding.cardPhotoPreview.setVisibility(View.GONE);
        });
    }

    private void setupSubmitButton() {
        binding.btnSubmit.setOnClickListener(v -> {
            if (validate()) submitDefect();
        });
    }

    // ── GPS ───────────────────────────────────────────────────────

    private void fetchGps() {
        binding.btnGetGps.setEnabled(false);
        binding.btnGetGps.setText("Определяю...");

        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        binding.btnGetGps.setEnabled(true);
                        binding.btnGetGps.setText("Обновить GPS");
                        if (location != null) {
                            pickedLat = location.getLatitude();
                            pickedLng = location.getLongitude();
                            updateCoordinatesUI();
                            // ← После GPS автоматически заполняем поле адреса
                            viewModel.reverseGeocode(pickedLat, pickedLng);
                        } else {
                            showToast("Не удалось получить координаты");
                        }
                    })
                    .addOnFailureListener(e -> {
                        binding.btnGetGps.setEnabled(true);
                        binding.btnGetGps.setText("Определить GPS");
                        showToast("Ошибка GPS: " + e.getMessage());
                    });
        } catch (SecurityException e) {
            binding.btnGetGps.setEnabled(true);
            binding.btnGetGps.setText("Определить GPS");
        }
    }

    // ── Камера ────────────────────────────────────────────────────

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraFileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    photoFile);
            cameraLauncher.launch(cameraFileUri);
        } catch (IOException e) {
            showToast("Не удалось создать файл для фото: " + e.getMessage());
        }
    }

    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        File storageDir = requireContext().getExternalFilesDir("Photos");
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile("DEFECT_" + timestamp, ".jpg", storageDir);
    }

    // ── Превью ────────────────────────────────────────────────────

    private void showPhotoPreview(Uri uri) {
        binding.ivPhotoPreview.setImageURI(uri);
        binding.cardPhotoPreview.setVisibility(View.VISIBLE);
    }

    // ── Валидация + Submit ────────────────────────────────────────

    private boolean validate() {
        boolean ok = true;

        String title = binding.etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.tilTitle.setError("Введите название дефекта");
            ok = false;
        } else {
            binding.tilTitle.setError(null);
        }

        if (viewModel.getDefectTypeCode() == null) {
            showToast("Выберите тип дефекта");
            ok = false;
        }

        // ── Обновлённая валидация адреса ────────────────────────────
        String address = binding.etAddress.getText().toString().trim();
        boolean hasCoords  = pickedLat != 0 || pickedLng != 0;
        boolean hasAddress = !TextUtils.isEmpty(address);

        if (!hasCoords && !hasAddress) {
            // Совсем ничего нет
            binding.tilAddress.setError("Укажите адрес или определите GPS");
            ok = false;
        } else if (hasAddress && !hasCoords) {
            // Текст есть, но координаты не подтверждены — запускаем геокодер
            binding.tilAddress.setError("Нажмите «Найти» чтобы проверить адрес");
            viewModel.geocodeAddress(address);
            ok = false;
        } else {
            binding.tilAddress.setError(null);
        }
        // ────────────────────────────────────────────────────────────

        return ok;
    }

    private void submitDefect() {
        Defect defect = new Defect();
        defect.setTitle(binding.etTitle.getText().toString().trim());
        defect.setDescription(binding.etDescription.getText().toString().trim());
        defect.setType(viewModel.getDefectTypeCode());
        defect.setSeverity(viewModel.getSeverity());
        defect.setAddress(binding.etAddress.getText().toString().trim());
        defect.setLatitude(pickedLat);
        defect.setLongitude(pickedLng);
        if (photoUri != null) {
            defect.setPhotoPath(photoUri.toString());
        }
        viewModel.createDefect(defect);
    }

    // ── ViewModel observers ───────────────────────────────────────

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSubmit.setEnabled(!loading);
            binding.btnSubmit.setText(loading ? "Сохраняю..." : "Создать дефект");
        });

        viewModel.getCreateResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result.status == Resource.Status.SUCCESS) {
                showToast("Дефект создан!");
                Navigation.findNavController(requireView()).popBackStack();
            } else if (result.status == Resource.Status.ERROR) {
                showToast("Ошибка: " + result.message);
            }
        });

        // ── Прямой геокодинг: адрес → координаты ────────────────────
        viewModel.getGeocodeResult().observe(getViewLifecycleOwner(), response -> {
            if (response == null) {
                binding.tilAddress.setError("Адрес не найден, проверьте написание");
                return;
            }
            GeocoderResponse.Point point = response.getFirstPoint();
            if (point == null) {
                binding.tilAddress.setError("Не удалось определить координаты");
                return;
            }
            pickedLat = point.getLatitude();
            pickedLng = point.getLongitude();

            // Заменяем поле адреса форматированным ответом от Яндекса
            String formatted = response.getFirstAddress();
            if (formatted != null) {
                // Снимаем watcher чтобы setText не сбросил pickedLat/pickedLng
                binding.etAddress.removeTextChangedListener(addressWatcher);
                binding.etAddress.setText(formatted);
                binding.etAddress.setSelection(formatted.length());
                binding.etAddress.addTextChangedListener(addressWatcher);
            }

            binding.tilAddress.setError(null);
            updateCoordinatesUI();
            showToast("Адрес подтверждён ✓");
        });

        // ── Обратный геокодинг: GPS → адрес ─────────────────────────
        viewModel.getReverseGeocodeResult().observe(getViewLifecycleOwner(), response -> {
            if (response == null) {
                // Координаты уже есть из fetchGps(), просто адрес не определился
                showToast("Координаты получены, адрес не определён");
                return;
            }
            String address = response.getFirstAddress();
            if (address != null) {
                // Снимаем watcher чтобы setText не сбросил координаты
                binding.etAddress.removeTextChangedListener(addressWatcher);
                binding.etAddress.setText(address);
                binding.etAddress.setSelection(address.length());
                binding.etAddress.addTextChangedListener(addressWatcher);
                binding.tilAddress.setError(null);
            }
            showToast("Адрес определён по GPS ✓");
        });

        // ── Прогресс геокодера ───────────────────────────────────────
        viewModel.isGeocodingLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressGeocoder.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnFindAddress.setEnabled(!loading);
        });
    }

    // ── Вспомогательные ──────────────────────────────────────────

    /**
     * TextWatcher в поле класса — чтобы можно было снимать/добавлять его
     * при программном setText из геокодера (иначе он сбросит координаты).
     */
    private final TextWatcher addressWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
            binding.tilAddress.setError(null);
            // Пользователь сам редактирует → старые координаты больше не актуальны
            pickedLat = 0;
            pickedLng = 0;
            binding.tvGpsCoords.setVisibility(View.GONE);
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    /** Обновляет строку с координатами под кнопкой GPS */
    private void updateCoordinatesUI() {
        binding.tvGpsCoords.setText(String.format("📍 %.5f, %.5f", pickedLat, pickedLng));
        binding.tvGpsCoords.setVisibility(View.VISIBLE);
    }
}