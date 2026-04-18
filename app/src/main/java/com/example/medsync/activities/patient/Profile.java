package com.example.medsync.activities.patient;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.medsync.R;
import com.example.medsync.utils.BaseProfileActivity;

public class Profile extends BaseProfileActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Set the layout (Patient uses the standard base profile layout)
        setContentView(R.layout.activity_base_profile);

        // 2. Setup standard BaseActivity features
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "My Profile");
        setupBaseActivityFooter("profile", "P");

        // 3. Find the reusable cards from activity_base_profile.xml
        View nameCard = findViewById(R.id.profile_name_card);
        View emailCard = findViewById(R.id.profile_email_card);
        View phoneCard = findViewById(R.id.profile_phone_card);
        View passCard = findViewById(R.id.profile_password_card);

        // 4. Load data (This triggers bindViewsFromData inside BaseProfileActivity)
        loadAuthProfileData(nameCard, emailCard, phoneCard, passCard);

        // 5. Setup Action Buttons
        setupLogout(findViewById(R.id.logout));

        // Initial setup for the profile circle
        updateProfileInitial();
    }

    private void updateProfileInitial() {
        TextView tvInitial = findViewById(R.id.tvUserInitial); // From layout_navbar if included or activity_base_profile
        if (tvInitial == null) tvInitial = findViewById(R.id.tvProfileInitial); // Check common ID

        if (tvInitial != null && user != null && user.getDisplayName() != null) {
            tvInitial.setText(user.getDisplayName().substring(0, 1).toUpperCase());
        }
    }

    @Override
    protected String getCollectionName() {
        return "patients"; // Collection path in Firestore
    }

    @Override
    protected String getUserRole() {
        return "P"; // Role code for footer and logic
    }
}