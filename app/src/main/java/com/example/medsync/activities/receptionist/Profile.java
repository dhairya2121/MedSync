package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.medsync.R;
import com.example.medsync.utils.BaseProfileActivity;

public class Profile extends BaseProfileActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Use the standard profile layout (compatible with BaseProfileActivity IDs)
        setContentView(R.layout.activity_base_profile);

        // 2. Standard BaseActivity setup
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "My Profile");
        setupBaseActivityFooter("profile", "R");

        // 3. Find the reusable cards defined in activity_base_profile.xml
        View nameCard = findViewById(R.id.profile_name_card);
        View emailCard = findViewById(R.id.profile_email_card);
        View phoneCard = findViewById(R.id.profile_phone_card);
        View passCard = findViewById(R.id.profile_password_card);

        // 4. Automated Loading (Handles Cache -> Bind -> Firebase Sync)
        loadAuthProfileData(nameCard, emailCard, phoneCard, passCard);

        // 5. Setup Action Buttons
        setupLogout(findViewById(R.id.logout));

        // Optional: Update initial icon
        updateProfileInitial();
    }

    private void updateProfileInitial() {
        TextView tvInitial = findViewById(R.id.tvProfileInitial);
        if (tvInitial != null && user != null && user.getDisplayName() != null) {
            tvInitial.setText(user.getDisplayName().substring(0, 1).toUpperCase());
        }
    }

    @Override
    protected String getCollectionName() {
        return "receptionists";
    }

    @Override
    protected String getUserRole() {
        return "R";
    }
}