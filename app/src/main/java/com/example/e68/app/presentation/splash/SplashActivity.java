package com.example.e68.app.presentation.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.e68.app.presentation.MainActivity;
import com.example.e68.app.R;
import com.example.e68.app.data.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    private View logoContainer;
    private LottieAnimationView lottieLogo;
    private View textContainer;
    private ProgressBar progressBar;
    private TextView tvVersion;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_E68_Splash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        startAnimations();

        // Простая задержка перед переходом
        mainHandler.postDelayed(this::navigate, 1500);
    }

    private void initViews() {
        logoContainer = findViewById(R.id.logoContainer);
        lottieLogo = findViewById(R.id.lottieLogo);
        textContainer = findViewById(R.id.textContainer);
        progressBar = findViewById(R.id.progressBar);
        tvVersion = findViewById(R.id.tvVersion);

        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + version);
        } catch (Exception e) {
            tvVersion.setText("v1.0.0");
        }
    }

    private void startAnimations() {
        Animation scaleFadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_scale_fade_in);
        logoContainer.startAnimation(scaleFadeIn);
        logoContainer.setVisibility(View.VISIBLE);

        lottieLogo.playAnimation();

        logoContainer.postDelayed(() -> {
            Animation slideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_slide_up_fade_in);
            textContainer.startAnimation(slideUpFadeIn);
            textContainer.setVisibility(View.VISIBLE);
        }, 800);
    }

    private void navigate() {
        Log.d("SplashActivity", "=== navigate ===");
        Log.d("SplashActivity", "sessionManager.isLoggedIn() = " + sessionManager.isLoggedIn());

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);

        // Если есть сессия, передаём роль
        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            Log.d("SplashActivity", "User has session, role: " + role);
            intent.putExtra("USER_ROLE", role);
        } else {
            Log.d("SplashActivity", "No session, going to login screen");
            // Не передаём роль - MainActivity покажет LoginFragment
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}