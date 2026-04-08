package com.example.medsync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.activities.dashboard.AssistantDashboard;
import com.example.medsync.activities.dashboard.CareTakerDashboard;
import com.example.medsync.activities.dashboard.DoctorDashboard;
import com.example.medsync.activities.dashboard.PatientDashboard;
import com.example.medsync.activities.dashboard.ReceptionistDashboard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (user != null) {
                // Already logged in — fetch role and redirect
                redirectByRole(user.getUid());
            } else {
                startActivity(new Intent(this, RoleSelectionActivity.class));
                finish();
            }
        }, 2000);
    }

    private void redirectByRole(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get().addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    Intent intent = getDashboardIntent(role);
                    startActivity(intent);
                    finish();
                });
    }

    private Intent getDashboardIntent(String role) {
        if (role == null) return new Intent(this, RoleSelectionActivity.class);
        switch (role) {
            case "doctor":       return new Intent(this, DoctorDashboard.class);
            case "assistant":    return new Intent(this, AssistantDashboard.class);
            case "patient":      return new Intent(this, PatientDashboard.class);
            case "caretaker":    return new Intent(this, CareTakerDashboard.class);
            default:             return new Intent(this, ReceptionistDashboard.class);
        }
    }
}