package com.example.medsync.activities.patient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Dashboard extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        // Apply system bar padding
        applyEdgeToEdgePadding(findViewById(R.id.main));

        // Initialize Navbar (Set "P" for Patient role)
        setupBaseActivityNavbar("P", "Patient Dashboard");

        // Initialize Footer
        setupBaseActivityFooter("home", "P");

        // Customizing the Navbar text to match the image style
        updateNavbarUI();


    }

    private void updateNavbarUI() {
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserSubtext = findViewById(R.id.tvUserSubtext);
        TextView tvUserInitial = findViewById(R.id.tvUserInitial);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName() != null ? user.getDisplayName() : "Patient User";
            tvUserName.setText(name);
            tvUserSubtext.setText("Patient Account");
            tvUserInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }
    public void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}