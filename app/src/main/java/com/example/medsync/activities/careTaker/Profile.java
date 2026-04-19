package com.example.medsync.activities.careTaker;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.medsync.R;
import com.example.medsync.utils.BaseProfileActivity;
public class Profile extends BaseProfileActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Set the layout (Customized patient layout with Gender/Age cards)
        setContentView(R.layout.activity_base_profile);

        // 2. Setup standard BaseActivity features
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("C", "Care Taker");
        setupBaseActivityFooter("profile", "C");

        // 3. Find the reusable cards
        View nameCard = findViewById(R.id.profile_name_card);
        View emailCard = findViewById(R.id.profile_email_card);
        View phoneCard = findViewById(R.id.profile_phone_card);
        View passCard = findViewById(R.id.profile_password_card);

        // 4. Load standard Auth data (Name, Email, Phone, Password)
        loadAuthProfileData(nameCard, emailCard, phoneCard, passCard);

        // 6. Setup Action Buttons
        setupLogout(findViewById(R.id.logout));

        // Initial setup for the profile circle
        updateProfileInitial();
    }

    @Override
    protected String getCollectionName() {
        return "careTakers"; // Matches your Firestore collection
    }

    @Override
    protected String getUserRole() {
        return "C"; // Used for footer/logic
    }

    private void updateProfileInitial() {
        TextView tvInitial = findViewById(R.id.tvProfileInitial);
        if (tvInitial != null && user != null && user.getDisplayName() != null) {
            if (!user.getDisplayName().isEmpty()) {
                tvInitial.setText(user.getDisplayName().substring(0, 1).toUpperCase());
            }
        }
    }

}