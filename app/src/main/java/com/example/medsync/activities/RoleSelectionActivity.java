package com.example.medsync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.medsync.R;

public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);
        LinearLayout btnReceptionist = findViewById(R.id.btnReceptionist);
        LinearLayout btnDoctor = findViewById(R.id.btnDoctor);
        LinearLayout btnAssistant = findViewById(R.id.btnAssistant);
        LinearLayout btnPatient = findViewById(R.id.btnPatient);
        LinearLayout btnCaretaker = findViewById(R.id.btnCaretaker);

        Intent signupIntent=new Intent(RoleSelectionActivity.this, SignupActivity.class);

        btnReceptionist.setOnClickListener(v -> {
            signupIntent.putExtra("role","R");
            startActivity(signupIntent);
            finish();
        });
        btnDoctor.setOnClickListener(v -> {
            signupIntent.putExtra("role","D");
            startActivity(signupIntent);
            finish();

        });
        btnAssistant.setOnClickListener(v -> {
            signupIntent.putExtra("role","A");
            startActivity(signupIntent);
            finish();

        });
        btnPatient.setOnClickListener(v -> {
            signupIntent.putExtra("role","P");
            startActivity(signupIntent);
            finish();

        });
        btnCaretaker.setOnClickListener(v -> {
            signupIntent.putExtra("role","C");
            startActivity(signupIntent);
            finish();

        });
    }

}