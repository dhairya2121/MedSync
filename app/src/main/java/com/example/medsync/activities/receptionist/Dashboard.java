package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;

public class Dashboard extends BaseActivity {
    String currPage="home";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receptionist_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home","R");

    }
}