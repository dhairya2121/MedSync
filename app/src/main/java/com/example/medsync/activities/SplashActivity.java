package com.example.medsync.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.medsync.activities.receptionist.Dashboard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            redirectByRole(user.getUid());
        } else {
            startActivity(new Intent(this, RoleSelectionActivity.class));
            finish();
        }
    }

    private void redirectByRole(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get().addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    startActivity(getDashboardIntent(role));
                    finish();
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, RoleSelectionActivity.class));
                    finish();
                });
    }

    private Intent getDashboardIntent(String role) {
        if (role == null) return new Intent(this, RoleSelectionActivity.class);
        switch (role) {
            case "doctor":       return new Intent(this, com.example.medsync.activities.doctor.Dashboard.class);
            case "assistant":    return new Intent(this, com.example.medsync.activities.assistant.Dashboard.class);
            case "patient":      return new Intent(this, com.example.medsync.activities.patient.Dashboard.class);
            case "caretaker":    return new Intent(this, com.example.medsync.activities.careTaker.Dashboard.class);
            default:             return new Intent(this, Dashboard.class);
        }
    }
}