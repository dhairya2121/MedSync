package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.adapters.TreatmentAdapter;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Treatment;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PatientDetails extends BaseActivity {

    private String patientId;
    private String hospitalId; // Added to store hospital context
    private FirebaseFirestore db;

    private TextView tvInitial, tvFullName;
    private RecyclerView rvTreatments;
    private List<Treatment> treatmentList;
    private TreatmentAdapter treatmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        db = FirebaseFirestore.getInstance();

        // 1. Get IDs from Intent and SharedPreferences
        patientId = getIntent().getStringExtra("patient_id");
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE)
                .getString("hospital_id", "");

        initViews();
        setupBaseActivityNavbar("R", "Patient Info");
        setupBaseActivityFooter("home", "R");

        if (patientId != null && !hospitalId.isEmpty()) {
            loadPatientData();
            loadPastTreatments();
        } else {
            Toast.makeText(this, "Missing Patient or Hospital ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        tvInitial = findViewById(R.id.tvProfileInitial);
        tvFullName = findViewById(R.id.tvPatientNameLarge);
        rvTreatments = findViewById(R.id.rvTreatments);

        treatmentList = new ArrayList<>();
        treatmentAdapter = new TreatmentAdapter(treatmentList);
        rvTreatments.setLayoutManager(new LinearLayoutManager(this));
        rvTreatments.setAdapter(treatmentAdapter);
    }

    private void loadPatientData() {
        db.collection("patients").document(patientId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Patient p = documentSnapshot.toObject(Patient.class);
                    if (p != null) {
                        tvFullName.setText(p.name);
                        tvInitial.setText(p.name != null && !p.name.isEmpty() ?
                                p.name.substring(0, 1).toUpperCase() : "P");

                        setupField(findViewById(R.id.email), p.email);
                        setupField(findViewById(R.id.phone), p.phone);
                        setupField(findViewById(R.id.gender), p.gender);
                        setupField(findViewById(R.id.age), String.valueOf(p.age));
                    }
                });
    }

    private void setupField(View root, String value) {
        if (root != null) {
            TextView tvField = root.findViewById(R.id.tv_field);
            if (tvField != null) {
                tvField.setText(value != null && !value.isEmpty() ? value : "N/A");
            }
        }
    }

    private void loadPastTreatments() {
        // 2. Updated path to look in specific hospital sub-collection
        // Path: hospitals/{hospitalId}/treatments
        db.collection("hospitals")
                .document(hospitalId)
                .collection("treatments")
                .whereEqualTo("patient_id", patientId)
                .orderBy("start", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null) {
                        treatmentList.clear();
                        treatmentList.addAll(queryDocumentSnapshots.toObjects(Treatment.class));
                        treatmentAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PatientDetails", "Error loading treatments: ", e);
                    Toast.makeText(this, "Failed to load treatment history", Toast.LENGTH_SHORT).show();
                });
    }
}