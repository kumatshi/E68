package com.example.e68.app.presentation.defects;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentEditDefectBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.example.e68.app.util.Resource;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditDefectFragment extends BaseFragment<FragmentEditDefectBinding> {

    private CreateDefectViewModel viewModel;
    private Defect currentDefect;
    private long defectId;

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

    @Override
    protected FragmentEditDefectBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                       @Nullable ViewGroup container) {
        return FragmentEditDefectBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CreateDefectViewModel.class);

        setupToolbar();
        setupDefectTypeChips();
        setupSeverityChips();
        setupSubmitButton();

        // Получаем ID дефекта из аргументов
        if (getArguments() != null) {
            defectId = getArguments().getLong("defectId", -1);
            if (defectId != -1) {
                loadDefect(defectId);
            } else {
                showToast("Дефект не найден");
                Navigation.findNavController(requireView()).popBackStack();
            }
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupDefectTypeChips() {
        for (String[] pair : DEFECT_TYPES) {
            String code = pair[0];
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
        binding.chipLow.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverity("LOW"); });
        binding.chipMedium.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverity("MEDIUM"); });
        binding.chipHigh.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverity("HIGH"); });
        binding.chipCritical.setOnCheckedChangeListener((b, c) -> { if (c) viewModel.setSeverity("CRITICAL"); });
    }

    private void loadDefect(long id) {
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);

        DefectsViewModel defectsViewModel = new ViewModelProvider(requireActivity()).get(DefectsViewModel.class);
        defectsViewModel.getAllDefects().observe(getViewLifecycleOwner(), defects -> {
            if (defects == null) return;
            for (Defect d : defects) {
                if (d.getId() == id) {
                    currentDefect = d;
                    bindDefectData(d);
                    return;
                }
            }
            binding.progressLoading.setVisibility(View.GONE);
            showToast("Дефект не найден");
        });
    }

    private void bindDefectData(Defect defect) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.scrollView.setVisibility(View.VISIBLE);

        // Название
        binding.etTitle.setText(defect.getTitle());

        // Описание
        if (defect.getDescription() != null && !defect.getDescription().isEmpty()) {
            binding.etDescription.setText(defect.getDescription());
        }

        // Тип дефекта
        if (defect.getType() != null) {
            String typeLabel = getTypeLabel(defect.getType());
            for (int i = 0; i < binding.defectTypeChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) binding.defectTypeChipGroup.getChildAt(i);
                if (chip.getText().toString().equals(typeLabel)) {
                    chip.setChecked(true);
                    viewModel.setDefectTypeCode(defect.getType());
                    break;
                }
            }
        }

        // Серьёзность
        if (defect.getSeverity() != null) {
            viewModel.setSeverity(defect.getSeverity());
            switch (defect.getSeverity()) {
                case "LOW":
                    binding.chipLow.setChecked(true);
                    break;
                case "MEDIUM":
                    binding.chipMedium.setChecked(true);
                    break;
                case "HIGH":
                    binding.chipHigh.setChecked(true);
                    break;
                case "CRITICAL":
                    binding.chipCritical.setChecked(true);
                    break;
            }
        }

        // Адрес
        if (defect.getAddress() != null && !defect.getAddress().isEmpty()) {
            binding.etAddress.setText(defect.getAddress());
        }
    }

    private void setupSubmitButton() {
        binding.btnSubmit.setOnClickListener(v -> {
            if (validate() && currentDefect != null) {
                updateDefect();
            }
        });
    }

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

        String address = binding.etAddress.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            binding.tilAddress.setError("Введите адрес");
            ok = false;
        } else {
            binding.tilAddress.setError(null);
        }

        return ok;
    }

    private void updateDefect() {
        if (currentDefect == null) return;

        // Обновляем все поля
        currentDefect.setTitle(binding.etTitle.getText().toString().trim());
        currentDefect.setDescription(binding.etDescription.getText().toString().trim());
        currentDefect.setType(viewModel.getDefectTypeCode());
        currentDefect.setSeverity(viewModel.getSeverity());
        currentDefect.setAddress(binding.etAddress.getText().toString().trim());
        currentDefect.setUpdatedAt(System.currentTimeMillis());

        // Добавьте логирование для проверки
        android.util.Log.d("EditDefectFragment", "Updating defect: title=" + currentDefect.getTitle()
                + ", type=" + currentDefect.getType()
                + ", severity=" + currentDefect.getSeverity());

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);

        DefectsViewModel defectsViewModel = new ViewModelProvider(requireActivity()).get(DefectsViewModel.class);
        defectsViewModel.updateDefect(currentDefect).observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.status == Resource.Status.SUCCESS) {
                android.util.Log.d("EditDefectFragment", "Update successful");
                showToast("Дефект обновлён!");
                Navigation.findNavController(requireView()).popBackStack();
            } else if (result.status == Resource.Status.ERROR) {
                android.util.Log.e("EditDefectFragment", "Update error: " + result.message);
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                showToast("Ошибка: " + result.message);
            }
        });
    }

    private String getTypeLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "PH_001": return "Выбоина";
            case "PH_002": return "Колея";
            case "PH_003": return "Трещина поперечная";
            case "PH_004": return "Трещина продольная";
            case "PH_005": return "Просадка";
            case "MK_001": return "Люк / решётка";
            case "MK_002": return "Бордюрный камень";
            case "SW_001": return "Светофор";
            case "SW_002": return "Дорожный знак";
            case "DR_001": return "Ливневая канализация";
            default: return type;
        }
    }
}