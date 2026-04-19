package com.example.medsync.activities.doctor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.example.medsync.activities.patient.HospitalDetails;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseSearchHospitalsActivity;

import java.util.Map;

public class SearchHospitals extends BaseSearchHospitalsActivity {

    @Override
    protected String getRoleCode() {
        return "D";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // After the base class initializes the Navbar with "Doctor",
        // we fetch the specific specialization and update it.
        fetchDynamicTitle();
    }

    private void fetchDynamicTitle() {
        if (mAuth.getUid() == null) return;

        db.collection("doctors").document(mAuth.getUid()).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String spec = document.getString("specialization");
                        if (spec != null && !spec.isEmpty()) {
                            try {
                                String displayName = SpecializationType.valueOf(spec).getDisplayName();
                                // Re-call the setup method to update the Subtext in Navbar
                                setupBaseActivityNavbar(getRoleCode(), displayName);
                            } catch (IllegalArgumentException e) {
                                // Fallback if enum mapping fails
                                setupBaseActivityNavbar(getRoleCode(), "Doctor");
                            }
                        }
                    }
                });
    }
    @Override
    protected void onHospitalClick(String hospitalId, Map<String, Object> data) {
        if (hospitalId != null) {
            linkHospitalToDoctor(hospitalId);

        }

    }
    private void linkHospitalToDoctor(String hospitalId) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("doctors").document(uid)
                .update("hospital_id", hospitalId)
                .addOnSuccessListener(aVoid -> {
                    // Save to local preferences so the Dashboard knows which hospital to load
                    SharedPreferences sharedPreferences = getSharedPreferences("medsync_prefs", MODE_PRIVATE);
                    sharedPreferences.edit()
                            .putString("hospital_id", hospitalId)
                            .apply();

                    Toast.makeText(this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, HospitalDetails.class);
                    intent.putExtra("hospital_id", hospitalId);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to join hospital: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}