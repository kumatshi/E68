package com.example.e68.app.presentation.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import androidx.constraintlayout.widget.ConstraintLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.example.e68.app.presentation.MainActivity;
import com.example.e68.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private View logoContainer;
    private LottieAnimationView lottieLogo;
    private View textContainer;
    private ProgressBar progressBar;
    private TextView tvVersion;

    private final long splashDelay = 2500L;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_E68_Splash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        initViews();
        startAnimations();
        checkUserAndNavigate();
    }

    private void initViews() {
        logoContainer = findViewById(R.id.logoContainer);
        lottieLogo = findViewById(R.id.lottieLogo);
        textContainer = findViewById(R.id.textContainer);
        progressBar = findViewById(R.id.progressBar);
        tvVersion = findViewById(R.id.tvVersion);

        // Устанавливаем версию приложения
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("v" + version);
        } catch (Exception e) {
            tvVersion.setText("v1.0.0");
        }
    }

    private void startAnimations() {
        startTime = System.currentTimeMillis();

        // Анимация для логотипа
        Animation scaleFadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_scale_fade_in);
        logoContainer.startAnimation(scaleFadeIn);
        logoContainer.setVisibility(View.VISIBLE);

        // Запускаем Lottie анимацию
        lottieLogo.playAnimation();

        // Анимация для текста с задержкой 800ms
        logoContainer.postDelayed(() -> {
            Animation slideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_slide_up_fade_in);
            textContainer.startAnimation(slideUpFadeIn);
            textContainer.setVisibility(View.VISIBLE);
        }, 800);
    }

    private void checkUserAndNavigate() {
        if (auth.getCurrentUser() == null) {
            // Пользователь не авторизован → переходим на MainActivity (покажет LoginFragment)
            navigateToMain(null);
        } else {
            // Пользователь авторизован → загружаем роль из Firestore
            loadUserRoleAndNavigate(auth.getCurrentUser().getUid());
        }
    }

    private void loadUserRoleAndNavigate(String uid) {
        showProgress(true);

        firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if (role == null || role.isEmpty()) {
                        role = "INSPECTOR";
                    }
                    navigateToMain(role);
                })
                .addOnFailureListener(e -> {
                    // При ошибке отправляем инспектора по умолчанию
                    navigateToMain("INSPECTOR");
                });
    }

    private void navigateToMain(String role) {
        // Ждём окончания анимации (минимум splashDelay)
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingDelay = Math.max(0, splashDelay - elapsedTime);

        mainHandler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (role != null) {
                intent.putExtra("USER_ROLE", role);
            }
            // В Activity, перед запуском новой
            try {
                startActivity(intent);
                // Если overridePendingTransition вызывается где-то в другом месте, закомментируйте его
            } catch (Exception e) {
                // Логируем ошибку, но не даем приложению упасть
                Log.e("AnimationError", "Failed to start activity", e);
                // Пробуем запустить без анимации
                startActivity(intent);
            }
            finish();
            overridePendingTransition(R.anim.splash_fade_in, android.R.anim.fade_out);
        }, remainingDelay);
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}