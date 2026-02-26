package com.example.e68.app.presentation.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.e68.app.R;
import com.example.e68.app.presentation.common.BaseFragment;
import com.example.e68.app.databinding.FragmentLoginBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends BaseFragment<FragmentLoginBinding> {

    private LoginViewModel viewModel;

    @Override
    protected FragmentLoginBinding inflateBinding(@NonNull LayoutInflater inflater,
                                                  @Nullable ViewGroup container) {
        return FragmentLoginBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.loginButton.setOnClickListener(v -> {
            String email    = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            viewModel.login(email, password);
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) showToast(error);
        });

        // После входа — разводим по ролям
        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;
            if (user.isInspector()) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_mainFragment);
            } else if (user.isManager()) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_managerFragment);
            } else if (user.isAdmin()) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_loginFragment_to_adminFragment);
            }
        });
    }

}
