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
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private View logoContainer;
    private LottieAnimationView lottieLogo;
    private View textContainer;
    private ProgressBar progressBar;
    private TextView tvVersion;

    private final long splashDelay = 2000L; // Уменьшил до 2 секунд
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long startTime;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_E68_Splash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        startAnimations();

        // Проверяем пользователя
        checkUserAndNavigate();
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
        startTime = System.currentTimeMillis();

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

    private void checkUserAndNavigate() {
        Log.d("SplashActivity", "=== checkUserAndNavigate ===");
        Log.d("SplashActivity", "sessionManager.isLoggedIn() = " + sessionManager.isLoggedIn());
        Log.d("SplashActivity", "auth.getCurrentUser() = " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "null"));

        // Проверяем сохранённую сессию
        if (sessionManager.isLoggedIn() && auth.getCurrentUser() != null) {
            String savedRole = sessionManager.getUserRole();
            Log.d("SplashActivity", "✅ User already logged in with role: " + savedRole);
            navigateToMain(savedRole);
        }
        else if (auth.getCurrentUser() != null) {
            Log.d("SplashActivity", "Firebase user exists but no session, loading role");
            loadUserRoleAndSaveSession(auth.getCurrentUser().getUid());
        }
        else {
            Log.d("SplashActivity", "❌ No user, going to login");
            navigateToMain(null);
        }
    }

    private void loadUserRoleAndSaveSession(String uid) {
        Log.d("SplashActivity", "loadUserRoleAndSaveSession for uid: " + uid);
        showProgress(true);

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (role == null || role.isEmpty()) {
                        role = "INSPECTOR";
                    }
                    Log.d("SplashActivity", "Saving session: uid=" + uid + ", role=" + role);
                    sessionManager.saveLoginSession(uid, role);
                    navigateToMain(role);
                })
                .addOnFailureListener(e -> {
                    Log.e("SplashActivity", "Failed to load role", e);
                    sessionManager.saveLoginSession(uid, "INSPECTOR");
                    navigateToMain("INSPECTOR");
                });
    }

    private void navigateToMain(String role) {
        if (isNavigating) {
            Log.d("SplashActivity", "Already navigating, skipping");
            return;
        }
        isNavigating = true;

        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingDelay = Math.max(0, splashDelay - elapsedTime);

        Log.d("SplashActivity", "navigateToMain called with role: " + role + ", delay: " + remainingDelay + "ms");

        mainHandler.postDelayed(() -> {
            Log.d("SplashActivity", "Starting MainActivity with role: " + role);
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (role != null) {
                intent.putExtra("USER_ROLE", role);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, remainingDelay);
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}