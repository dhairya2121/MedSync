package com.example.medsync.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.activities.dashboard.AssistantDashboard;
import com.example.medsync.activities.dashboard.DoctorDashboard;
import com.example.medsync.activities.dashboard.PatientDashboard;
import com.example.medsync.activities.dashboard.ReceptionistDashboard;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        MaterialButton loginBtn = findViewById(R.id.loginBtn);
        EditText emailEt = findViewById(R.id.etEmail);
        EditText passEt = findViewById(R.id.etPass);
        TextView signupLinkBtn = findViewById(R.id.tvSignupLink);

        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String pass = passEt.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            ViewUtils.setLoading(LoginActivity.this, true, loginBtn, "Verifying...", "Login");

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Logged In Successfully!", Toast.LENGTH_SHORT).show();
                            redirectToRoleBasedDashboard(user);
                        } else {
                            ViewUtils.setLoading(LoginActivity.this, false, loginBtn, "", "Login");
                            String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        signupLinkBtn.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RoleSelectionActivity.class));
            finish();
        });
    }

    public void redirectToRoleBasedDashboard(FirebaseUser user) {
        if (user == null) return;

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
                            Intent intent = new Intent(LoginActivity.this, targetActivity);
                            startActivity(intent);
                            finish();
                        } else {
                            // If role is in DB but doesn't match P, D, R, or A
                            Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Profile not found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user document", e);
                    Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            redirectToRoleBasedDashboard(currentUser);
        }
    }
}