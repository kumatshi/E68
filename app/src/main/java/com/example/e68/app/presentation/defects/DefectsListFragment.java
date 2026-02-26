package com.example.e68.app.presentation.defects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.e68.app.R;
import com.example.e68.app.databinding.FragmentDefectsListBinding;
import com.example.e68.app.domain.entity.Defect;
import com.example.e68.app.presentation.common.BaseFragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;

@AndroidEntryPoint
public class DefectsListFragment extends BaseFragment<FragmentDefectsListBinding> {

    private DefectsViewModel viewModel;
    private DefectsAdapter adapter;
    private String currentFilter = "ALL";

    @Override
    protected FragmentDefectsListBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                        @Nullable ViewGroup container) {
        return FragmentDefectsListBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DefectsViewModel.class);

        setupRecyclerView();
        setupFilters();
        setupFab();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new DefectsAdapter(defect -> {
            // Навигация к деталям дефекта через Bundle (без SafeArgs/Directions)
            Bundle args = new Bundle();
            args.putLong("defectId", defect.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_defectsList_to_defectDetail, args);
        });
        binding.defectsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.defectsRecyclerView.setAdapter(adapter);
        binding.defectsRecyclerView.setHasFixedSize(false);
    }

    private void setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipAll)        currentFilter = "ALL";
            else if (id == R.id.chipOpen)  currentFilter = "OPEN";
            else if (id == R.id.chipInProgress) currentFilter = "IN_PROGRESS";
            else if (id == R.id.chipResolved)   currentFilter = "RESOLVED";
            viewModel.setFilter(currentFilter);
        });
    }

    private void setupFab() {
        binding.fabCreateDefect.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_defectsList_to_createDefect));

        binding.defectsRecyclerView.addOnScrollListener(
                new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView rv,
                                   int dx, int dy) {
                if (dy > 4)       binding.fabCreateDefect.shrink();
                else if (dy < -4) binding.fabCreateDefect.extend();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getFilteredDefects().observe(getViewLifecycleOwner(), defects -> {
            adapter.submitList(defects);
            binding.emptyState.setVisibility(defects.isEmpty() ? View.VISIBLE : View.GONE);
            binding.defectsRecyclerView.setVisibility(
                    defects.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getAllDefects().observe(getViewLifecycleOwner(), this::updateStatsBadges);

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) showToast(error);
        });
    }

    private void updateStatsBadges(List<Defect> defects) {
        if (defects == null) return;
        long open       = defects.stream().filter(d -> "OPEN".equals(d.getStatus())).count();
        long inProgress = defects.stream().filter(d -> "IN_PROGRESS".equals(d.getStatus())).count();
        long resolved   = defects.stream().filter(d -> "RESOLVED".equals(d.getStatus())).count();
        binding.countOpen.setText(String.valueOf(open));
        binding.countInProgress.setText(String.valueOf(inProgress));
        binding.countResolved.setText(String.valueOf(resolved));
    }
}
