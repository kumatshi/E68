package com.example.e68.app.presentation.defects;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentDefectDetailBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import com.example.e68.app.util.Resource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DefectDetailFragment extends BaseFragment<FragmentDefectDetailBinding> {

    private DefectsViewModel viewModel;
    private Defect currentDefect;

    @Override
    protected FragmentDefectDetailBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                         @Nullable ViewGroup container) {
        return FragmentDefectDetailBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DefectsViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        long defectId = getArguments() != null ? getArguments().getLong("defectId", -1) : -1;
        if (defectId != -1) {
            loadDefect(defectId);
        } else {
            showToast("Дефект не найден");
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOAD
    // ═══════════════════════════════════════════════════════════

    private void loadDefect(long id) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);

        viewModel.getAllDefects().observe(getViewLifecycleOwner(), defects -> {
            if (defects == null) return;
            for (Defect d : defects) {
                if (d.getId() == id) {
                    currentDefect = d;
                    bindDefect(d);
                    return;
                }
            }
            binding.progressBar.setVisibility(View.GONE);
            showToast("Дефект не найден");
        });
    }

    // ═══════════════════════════════════════════════════════════
    // BIND
    // ═══════════════════════════════════════════════════════════

    private void bindDefect(Defect d) {
        binding.progressBar.setVisibility(View.GONE);
        binding.contentLayout.setVisibility(View.VISIBLE);

        binding.toolbar.setTitle(d.getTitle() != null ? d.getTitle() : "Дефект");

        binding.tvTitle.setText(d.getTitle());
        binding.tvDescription.setText(
                d.getDescription() != null && !d.getDescription().isEmpty()
                        ? d.getDescription() : "Описание не указано");

        binding.tvType.setText(getTypeLabel(d.getType()));

        binding.tvSeverity.setText(getSeverityLabel(d.getSeverity()));
        binding.tvSeverity.setTextColor(getSeverityColor(d.getSeverity()));

        StatusHelper.applyStatus(binding.tvStatus, d.getStatus());

        if (d.getAddress() != null && !d.getAddress().isEmpty()) {
            binding.tvAddress.setText(d.getAddress());
        } else if (d.getLatitude() != 0 || d.getLongitude() != 0) {
            binding.tvAddress.setText(String.format(Locale.getDefault(),
                    "%.5f, %.5f", d.getLatitude(), d.getLongitude()));
        } else {
            binding.tvAddress.setText("Адрес не указан");
        }

        if (d.getLatitude() != 0 || d.getLongitude() != 0) {
            binding.tvCoords.setText(String.format(Locale.getDefault(),
                    "%.5f° с.ш., %.5f° в.д.", d.getLatitude(), d.getLongitude()));
            binding.rowCoords.setVisibility(View.VISIBLE);
        } else {
            binding.rowCoords.setVisibility(View.GONE);
        }

        binding.tvCreatedBy.setText(d.getCreatedBy() != null ? d.getCreatedBy() : "—");
        binding.tvCreatedAt.setText(new SimpleDateFormat("dd MMMM yyyy, HH:mm",
                new Locale("ru")).format(new Date(d.getCreatedAt())));

        loadPhoto(d.getPhotoPath());

        binding.btnChangeStatus.setOnClickListener(v -> showStatusDialog(d));
    }

    // ═══════════════════════════════════════════════════════════
    // ФОТО
    // ═══════════════════════════════════════════════════════════

    private void loadPhoto(@Nullable String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            showNoPhoto();
            return;
        }

        try {
            Uri uri = Uri.parse(photoPath);
            binding.cardPhoto.setVisibility(View.VISIBLE);
            binding.cardNoPhoto.setVisibility(View.GONE);

            Glide.with(requireContext())
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.ic_camera)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                @Nullable com.bumptech.glide.load.engine.GlideException e,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            // URI недоступен — показываем заглушку
                            showNoPhoto();
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(
                                android.graphics.drawable.Drawable resource,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource,
                                boolean isFirstResource) {
                            // Фото загружено — карточка уже видна
                            return false;
                        }
                    })
                    .into(binding.ivPhoto);

        } catch (Exception e) {
            showNoPhoto();
        }
    }

    private void showNoPhoto() {
        if (getView() == null) return;
        binding.cardPhoto.setVisibility(View.GONE);
        binding.cardNoPhoto.setVisibility(View.VISIBLE);
    }

    // ═══════════════════════════════════════════════════════════
    // СМЕНА СТАТУСА
    // ═══════════════════════════════════════════════════════════

    private void showStatusDialog(Defect defect) {
        String[] statuses = {"OPEN", "IN_PROGRESS", "RESOLVED", "REJECTED"};
        String[] labels   = {"Открыт", "В работе", "Устранён", "Отклонён"};

        int currentIndex = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equals(defect.getStatus())) { currentIndex = i; break; }
        }
        final int current = currentIndex;

        new AlertDialog.Builder(requireContext())
                .setTitle("Изменить статус")
                .setSingleChoiceItems(labels, current, null)
                .setPositiveButton("Сохранить", (dialog, w) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected == current) return;
                    defect.setStatus(statuses[selected]);
                    viewModel.updateDefect(defect).observe(getViewLifecycleOwner(), result -> {
                        if (result == null) return;
                        if (result.status == Resource.Status.SUCCESS) {
                            StatusHelper.applyStatus(binding.tvStatus, defect.getStatus());
                            showToast("Статус обновлён");
                        } else if (result.status == Resource.Status.ERROR) {
                            showToast("Ошибка: " + result.message);
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private String getTypeLabel(String type) {
        if (type == null) return "Неизвестно";
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
            default:       return type;
        }
    }

    private String getSeverityLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "LOW":      return "Низкая";
            case "MEDIUM":   return "Средняя";
            case "HIGH":     return "Высокая";
            case "CRITICAL": return "Критическая";
            default:         return s;
        }
    }

    private int getSeverityColor(String s) {
        if (s == null) return Color.GRAY;
        switch (s) {
            case "LOW":      return Color.parseColor("#06D6A0");
            case "MEDIUM":   return Color.parseColor("#FFD166");
            case "HIGH":     return Color.parseColor("#FF6B35");
            case "CRITICAL": return Color.parseColor("#FF4757");
            default:         return Color.GRAY;
        }
    }
}