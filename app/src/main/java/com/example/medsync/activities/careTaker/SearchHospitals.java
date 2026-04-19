package com.example.medsync.activities.careTaker;

import android.content.Intent;
import com.example.medsync.activities.patient.HospitalDetails;
import com.example.medsync.utils.BaseSearchHospitalsActivity;
import java.util.Map;

public class SearchHospitals extends BaseSearchHospitalsActivity {

    @Override
    protected String getRoleCode() { return "C"; }

    @Override
    protected void onHospitalClick(String hospitalId, Map<String, Object> data) {
        Intent intent = new Intent(this, HospitalDetails.class);
        intent.putExtra("hospital_id", hospitalId);
        startActivity(intent);
    }
}