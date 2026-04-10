package com.example.medsync.activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import java.util.HashMap;
import java.util.Map;

// ... (keep existing imports)

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Handle Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        String role = getIntent().getStringExtra("role");

        MaterialButton signupBtn = findViewById(R.id.signupBtn);
        EditText nameEt = findViewById(R.id.etName);
        EditText emailEt = findViewById(R.id.etEmail);
        EditText passEt = findViewById(R.id.etPass);
        EditText confirmPassEt = findViewById(R.id.etConfirmPass);

        signupBtn.setOnClickListener(v -> {
            String name = nameEt.getText().toString().trim();
            String email = emailEt.getText().toString().trim();
            String pass = passEt.getText().toString().trim();
            String confirmPass = confirmPassEt.getText().toString().trim();

            TextInputLayout confirmPassLayout = findViewById(R.id.tlConfirmPass);

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role == null) {
                Toast.makeText(SignupActivity.this, "Error: Role missing", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                confirmPassLayout.setError("Passwords do not match");
                confirmPassEt.requestFocus();
            } else {
                confirmPassLayout.setError(null);
                ViewUtils.setLoading(SignupActivity.this, true, signupBtn, "Creating Account...", "Sign Up");
                signupUser(signupBtn, name, email, pass, role);
            }
        });

        findViewById(R.id.tvLoginLink).setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    public void signupUser(MaterialButton signupBtn, String name, String email, String pass, String role) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, signupTask -> {
                    if (signupTask.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;

                        // 1. Update Profile (Name)
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name).build();

                        user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                            // 2. Save Role to Firestore (Wait for success before redirecting)
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("role", role);
                            userMap.put("name", name); // Also save name to DB for easy access

                            db.collection("users").document(user.getUid()).set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        ViewUtils.setLoading(SignupActivity.this, false, signupBtn, "", "Sign Up");
                                        Toast.makeText(SignupActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                                        // 3. NOW it is safe to redirect
                                        ViewUtils.redirectToRoleBasedDashboard(SignupActivity.this,user);
                                    })
                                    .addOnFailureListener(e -> {
                                        ViewUtils.setLoading(SignupActivity.this, false, signupBtn, "", "Sign Up");
                                        Toast.makeText(SignupActivity.this, "Database Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });
                    } else {
                        ViewUtils.setLoading(SignupActivity.this, false, signupBtn, "", "Sign Up");
                        String error = signupTask.getException() != null ? signupTask.getException().getMessage() : "Signup Failed";
                        Toast.makeText(SignupActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

}