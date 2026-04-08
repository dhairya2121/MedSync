package com.example.medsync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.medsync.R;
import com.example.medsync.activities.auth.LoginActivity; // Assuming your login path

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);

        // Initialize buttons
        LinearLayout btnReceptionist = findViewById(R.id.btnReceptionist);
        LinearLayout btnDoctor = findViewById(R.id.btnDoctor);
        LinearLayout btnAssistant = findViewById(R.id.btnAssistant);
        LinearLayout btnPatient = findViewById(R.id.btnPatient);
        LinearLayout btnCaretaker = findViewById(R.id.btnCaretaker);

        // Set Click Listeners
        btnReceptionist.setOnClickListener(v -> navigateToLogin("receptionist"));
        btnDoctor.setOnClickListener(v -> navigateToLogin("doctor"));
        btnAssistant.setOnClickListener(v -> navigateToLogin("assistant"));
        btnPatient.setOnClickListener(v -> navigateToLogin("patient"));
        btnCaretaker.setOnClickListener(v -> navigateToLogin("caretaker"));
    }

    private void navigateToLogin(String role) {
        // Pass the selected role to the next activity
        Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
        intent.putExtra("SELECTED_ROLE", role);
        startActivity(intent);
    }
}