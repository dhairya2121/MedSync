package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.content.Intent;import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.example.medsync.R;
import com.example.medsync.model.CareTaker;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class CareTakerDetails extends BaseActivity {
    private FirebaseFirestore db;
    private String careTakerId;
    private String assignedPatientId;
    private String assignedPatientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_care_taker_details);

        db = FirebaseFirestore.getInstance();
        careTakerId = getIntent().getStringExtra("care_taker_id");

        setupBaseActivityNavbar("R", "Care Taker Profile");
        setupBaseActivityFooter("home", "R");

        loadCareTakerData();

        findViewById(R.id.btn_view_patient).setOnClickListener(v -> {
            if (assignedPatientId != null) {
                Intent intent = new Intent(this, PatientDetails.class);
                intent.putExtra("patient_id", assignedPatientId);
                intent.putExtra("patient_name", assignedPatientName);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_remove_caretaker).setOnClickListener(v -> {
            db.collection("careTakers").document(careTakerId).delete()
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Care Taker Removed", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });
    }

    private void loadCareTakerData() {
        db.collection("careTakers").document(careTakerId).get().addOnSuccessListener(doc -> {
            CareTaker ct = doc.toObject(CareTaker.class);
            if (ct != null) {
                ((TextView)findViewById(R.id.tvCareTakerName)).setText(ct.name);
                ((TextView)findViewById(R.id.tvProfileInitial)).setText(ct.name.substring(0,1).toUpperCase());

                setupField(findViewById(R.id.view_email), ct.email);
                setupField(findViewById(R.id.view_phone), ct.phone);

                assignedPatientId = ct.patient_id;
                assignedPatientName = ct.patient_name;
                ((MaterialButton)findViewById(R.id.btn_view_patient)).setText("Patient: " + (isValidUid(ct.patient_name)?ct.patient_name:"None"));
            }
        });
    }

    private void setupField(android.view.View root, String value) {
        if (root != null) {
            ((TextView) root.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
        }
    }
}