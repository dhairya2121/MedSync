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
//        mAuth.signOut(); //for debugging
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
                            ViewUtils.redirectToRoleBasedDashboard(LoginActivity.this, user);
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

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

            ViewUtils.redirectToRoleBasedDashboard(LoginActivity.this,currentUser);
        }
    }
}