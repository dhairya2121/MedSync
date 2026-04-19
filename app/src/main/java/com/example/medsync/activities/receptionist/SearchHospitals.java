package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.utils.BaseSearchHospitalsActivity;

import java.util.HashMap;
import java.util.Map;

public class SearchHospitals extends BaseSearchHospitalsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Receptionist specific: Enable and handle "Add New" Button
        View btnAddNew = findViewById(R.id.btn_add_new);
        if (btnAddNew != null) {
            btnAddNew.setVisibility(View.VISIBLE);
            btnAddNew.setOnClickListener(v -> createNewHospitalAndLink());
        }
    }

    @Override
    protected String getRoleCode() { return "R"; }

    @Override
    protected void onHospitalClick(String hospitalId, Map<String, Object> data) {
        if (hospitalId != null) {
            linkHospitalToReceptionist(hospitalId);
        }
    }

    private void createNewHospitalAndLink() {
        Map<String, Object> dummyHospital = new HashMap<>();
        dummyHospital.put("legal_name", "New Hospital Name");
        dummyHospital.put("address", "Enter Address");
        dummyHospital.put("phone", "");
        dummyHospital.put("email", "");
        dummyHospital.put("rating", 0.0);
        dummyHospital.put("reviewCount", 0);

        db.collection("hospitals")
                .add(dummyHospital)
                .addOnSuccessListener(ref -> linkHospitalToReceptionist(ref.getId()))
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create hospital", Toast.LENGTH_SHORT).show());
    }

    private void linkHospitalToReceptionist(String hospitalId) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("receptionists").document(uid)
                .update("hospital_id", hospitalId)
                .addOnSuccessListener(aVoid -> {
                    getSharedPreferences("medsync_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("hospital_id", hospitalId)
                            .apply();

                    startActivity(new Intent(this, Hospital.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to link hospital", Toast.LENGTH_SHORT).show());
    }
}