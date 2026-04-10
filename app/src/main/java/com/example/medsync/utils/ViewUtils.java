package com.example.medsync.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.example.medsync.activities.dashboard.AssistantDashboard;
import com.example.medsync.activities.dashboard.DoctorDashboard;
import com.example.medsync.activities.dashboard.PatientDashboard;
import com.example.medsync.activities.dashboard.ReceptionistDashboard;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

public class ViewUtils {

    public static void setLoading(Context context, boolean isLoading, MaterialButton btn, String loadingText, String defaultText) {
        if (isLoading) {
            CircularProgressDrawable progressDrawable = new CircularProgressDrawable(context);
            progressDrawable.setStrokeWidth(5f);
            progressDrawable.setCenterRadius(20f);
            progressDrawable.setColorSchemeColors(Color.WHITE);
            progressDrawable.start();

            btn.setIconTint(null);
            btn.setIcon(progressDrawable);
            btn.setText(loadingText);
            btn.setEnabled(false);
        } else {
            btn.setIcon(null);
            btn.setText(defaultText);
            btn.setEnabled(true);
        }
    }

    public static void redirectToRoleBasedDashboard(Activity activity, FirebaseUser user) {
        if (user == null || activity == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        Class<?> targetActivity = null;

                        if (role != null) {
                            switch (role) {


                                case "P": targetActivity = PatientDashboard.class; break;
                                case "D": targetActivity = DoctorDashboard.class; break;
                                case "R": targetActivity = ReceptionistDashboard.class; break;
                                case "A": targetActivity = AssistantDashboard.class; break;
                            }
                        }

                        if (targetActivity != null) {
                            Intent intent = new Intent(activity, targetActivity);
                            activity.startActivity(intent);
                            activity.finish(); // Close the calling activity (Login/Signup)
                        } else {
                            Toast.makeText(activity, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Profile not found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewUtils", "Error fetching role", e);
                    Toast.makeText(activity, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                });
    }


}