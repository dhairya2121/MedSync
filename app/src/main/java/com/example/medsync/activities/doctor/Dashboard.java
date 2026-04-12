package com.example.medsync.activities.doctor;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medsync.R;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Dashboard extends BaseActivity {
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);
        applyEdgeToEdgePadding(findViewById(R.id.main));

        mAuth=FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        db.collection("doctors").document(user.getUid()).get()
                .addOnSuccessListener(Dashboard.this,d->{
                    if(d.exists()) {
                        String spec = d.getString("specialization");
                        setupBaseActivityNavbar("D", spec);
                    }
                    else{
                        setupBaseActivityNavbar("D", SpecializationType.GENERAL_PHYSICIAN.getDisplayName());
                    }
                })
                .addOnFailureListener(e->setupBaseActivityNavbar("D", SpecializationType.GENERAL_PHYSICIAN.getDisplayName()));

        setupBaseActivityFooter("home","D");
    }
}