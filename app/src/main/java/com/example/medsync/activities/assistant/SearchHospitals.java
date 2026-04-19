package com.example.medsync.activities.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.activities.patient.HospitalDetails;
import com.example.medsync.activities.receptionist.Hospital;
import com.example.medsync.utils.BaseSearchHospitalsActivity;

import java.util.HashMap;
import java.util.Map;

public class SearchHospitals extends BaseSearchHospitalsActivity {

    @Override
    protected String getRoleCode() { return "A"; }


    @Override
    protected void onHospitalClick(String hospitalId, Map<String, Object> data) {
        if (hospitalId != null) {
            linkHospitalToAssistant(hospitalId);
        }
    }

    private void linkHospitalToAssistant(String hospitalId) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("assistants").document(uid)
                .update("hospital_id", hospitalId)
                .addOnSuccessListener(aVoid -> {
                    getSharedPreferences("medsync_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("hospital_id", hospitalId)
                            .apply();
                    Intent intent=new Intent(this, HospitalDetails.class);
                    intent.putExtra("hospital_id",hospitalId);
                    startActivity(intent);
                    Toast.makeText(this, "Linked Hospital", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to link hospital", Toast.LENGTH_SHORT).show());
    }
}