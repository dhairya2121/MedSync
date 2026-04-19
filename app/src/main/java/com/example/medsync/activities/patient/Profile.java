package com.example.medsync.activities.patient;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.utils.BaseProfileActivity;
import com.example.medsync.utils.ViewUtils;

public class Profile extends BaseProfileActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Set the layout (Customized patient layout with Gender/Age cards)
        setContentView(R.layout.activity_patient_profile);

        // 2. Setup standard BaseActivity features
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "My Profile");
        setupBaseActivityFooter("profile", "P");

        // 3. Find the reusable cards
        View nameCard = findViewById(R.id.profile_name_card);
        View emailCard = findViewById(R.id.profile_email_card);
        View phoneCard = findViewById(R.id.profile_phone_card);
        View passCard = findViewById(R.id.profile_password_card);
        View genderCard = findViewById(R.id.profile_gender_card);
        View ageCard = findViewById(R.id.profile_age_card);

        // 4. Load standard Auth data (Name, Email, Phone, Password)
        loadAuthProfileData(nameCard, emailCard, phoneCard, passCard);

        // 5. Load and Setup Patient specific data (Gender, Age)
        loadGenderAndAgeData(genderCard, ageCard);
        setupGenderAndAgeCard(genderCard, ageCard);

        // 6. Setup Action Buttons
        setupLogout(findViewById(R.id.logout));

        // Initial setup for the profile circle
        updateProfileInitial();
    }

    public void loadGenderAndAgeData(View genderCard, View ageCard) {
        if (user == null) return;

        db.collection("patients").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String gender = doc.getString("gender");
                        Long age = doc.getLong("age");

                        // Update the internal EditTexts directly so the values show up
                        EditText etGender = genderCard.findViewById(R.id.edit_text_input);
                        if (etGender != null) etGender.setText(gender != null ? gender : "Select Gender");

                        EditText etAge = ageCard.findViewById(R.id.edit_text_input);
                        if (etAge != null) etAge.setText(age != null ? String.valueOf(age) : "Set Age");
                    }
                });
    }

    public void setupGenderAndAgeCard(View genderCard, View ageCard) {
        // Setup Gender Card
        ViewUtils.setupEditableInfoCard(this, genderCard, R.drawable.ic_doctor, "Gender",
                "", val -> {
                    ViewUtils.setInputState(this, genderCard, "LOADING");
                    db.collection("patients").document(user.getUid()).update("gender", val)
                            .addOnCompleteListener(dbTask -> {
                                ViewUtils.setInputState(this, genderCard, dbTask.isSuccessful() ? "IDLE" : "ERROR");
                                if (dbTask.isSuccessful()) {
                                    invalidateProfileCache();
                                    Toast.makeText(this, "Gender Saved", Toast.LENGTH_SHORT).show();
                                }
                            });
                });

        // Setup Age Card
        ViewUtils.setupEditableInfoCard(this, ageCard, R.drawable.ic_clock, "Age",
                "", val -> {
                    if (val.isEmpty()) return;

                    ViewUtils.setInputState(this, ageCard, "LOADING");
                    try {
                        long ageVal = Long.parseLong(val);
                        db.collection("patients").document(user.getUid()).update("age", ageVal)
                                .addOnCompleteListener(dbTask -> {
                                    // FIX: Changed genderCard to ageCard here to prevent sticking UI
                                    ViewUtils.setInputState(this, ageCard, dbTask.isSuccessful() ? "IDLE" : "ERROR");
                                    if (dbTask.isSuccessful()) {
                                        invalidateProfileCache();
                                        Toast.makeText(this, "Age Saved", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } catch (NumberFormatException e) {
                        ViewUtils.setInputState(this, ageCard, "ERROR");
                        Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProfileInitial() {
        TextView tvInitial = findViewById(R.id.tvProfileInitial);
        if (tvInitial != null && user != null && user.getDisplayName() != null) {
            if (!user.getDisplayName().isEmpty()) {
                tvInitial.setText(user.getDisplayName().substring(0, 1).toUpperCase());
            }
        }
    }

    @Override
    protected String getCollectionName() {
        return "patients";
    }

    @Override
    protected String getUserRole() {
        return "P";
    }
}