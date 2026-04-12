package com.example.medsync.activities.assistant;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Dashboard extends BaseActivity {
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("A","Assistant");
        setupBaseActivityFooter("home","A");


    }
}