package com.example.medsync.activities.patient;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;

public class Dashboard extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P","Patient");
        setupBaseActivityFooter("home","P");

    }
}