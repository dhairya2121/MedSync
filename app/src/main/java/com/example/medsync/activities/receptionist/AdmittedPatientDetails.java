package com.example.medsync.activities.receptionist;import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Patient;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdmittedPatientDetails extends BaseActivity {

    private String patientId,patientName,doctorName;
    private FirebaseFirestore db;
    private TextView tvInitial, tvFullName;
    private MaterialButton btnDischarge,btnScheduleOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admitted_patient_details);

        db = FirebaseFirestore.getInstance();
        patientId = getIntent().getStringExtra("patient_id");

        initViews();
        setupBaseActivityNavbar("R", "Admitted Details");
        setupBaseActivityFooter("home", "R");

        if (patientId != null) {
            loadPatientData();
        } else {
            Toast.makeText(this, "Error: Patient ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvInitial = findViewById(R.id.tvProfileInitial);
        tvFullName = findViewById(R.id.tvPatientNameLarge);
        btnDischarge = findViewById(R.id.btnDischarge);
        btnScheduleOperation=findViewById(R.id.btnScheduleOperation);
        btnDischarge.setOnClickListener(v -> {
            // Redirect to future discharge activity
            // Intent intent = new Intent(this, DischargeProcessActivity.class);
            // intent.putExtra("patient_id", patientId);
            // startActivity(intent);
            Toast.makeText(this, "Proceeding to Discharge...", Toast.LENGTH_SHORT).show();
        });
        btnScheduleOperation.setOnClickListener(v->{
            Intent intent = new Intent(this, ScheduleOperation.class);
            intent.putExtra("PATIENT_ID", patientId);
            intent.putExtra("PATIENT_NAME", patientName);
            intent.putExtra("DOCTOR_NAME", doctorName); // Use whatever field holds the doctor name
            startActivity(intent);

        });
    }

    private void loadPatientData() {
        db.collection("patients").document(patientId)
                .get()
                .addOnSuccessListener(doc -> {
                    Patient p = doc.toObject(Patient.class);
                    if (p != null) {
                        tvFullName.setText(p.name);
                        tvInitial.setText(p.name != null && !p.name.isEmpty() ?
                                p.name.substring(0,1).toUpperCase() : "P");
                        patientName=p.name;
                        if(p.doctor_id!=null){
                            db.collection("doctors").document(p.doctor_id).addSnapshotListener((doctorDoc,err)-> {
                                if (err != null) return;
                                if (doctorDoc != null) {
                                    doctorName = doctorDoc.getString("name");

                                    if (!ViewUtils.isValidUid(doctorName)) {
                                        doctorName = "N/A";
                                    }
                                }
                            });
                        }

                        // Reusing the setupField logic from your PatientDetails activity
                        setupField(findViewById(R.id.roomNo), "Room " + p.room_no);
                        setupField(findViewById(R.id.admittedDate), "Admitted: " + p.admittedOn);
                        setupField(findViewById(R.id.gender), p.gender);
                        setupField(findViewById(R.id.age), p.age + " yrs");

                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void setupField(View root, String value) {
        if (root != null) {
            TextView tvField = root.findViewById(R.id.tv_field);
            if (tvField != null) {
                tvField.setText(value != null && !value.isEmpty() ? value : "N/A");
            }
        }
    }
}