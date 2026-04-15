package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;import androidx.appcompat.app.AlertDialog;
import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class DoctorDetails extends BaseActivity {

    private FirebaseFirestore db;
    private String doctorId;
    private TextView tvInitial, tvFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details);

        db = FirebaseFirestore.getInstance();
        doctorId = getIntent().getStringExtra("doctor_id");

        setupBaseActivityNavbar("R", "Doctor Profile");
        setupBaseActivityFooter("home", "R");

        tvInitial = findViewById(R.id.tvProfileInitial);
        tvFullName = findViewById(R.id.tvDoctorNameLarge);

        if (doctorId != null) {
            loadDoctorData();
        }

        findViewById(R.id.btn_remove_doctor).setOnClickListener(v -> confirmRemoval());
    }

    private void loadDoctorData() {
        db.collection("doctors").document(doctorId).get()
                .addOnSuccessListener(doc -> {
                    Doctor d = doc.toObject(Doctor.class);
                    if (d != null) {
                        tvFullName.setText("Dr. " + d.name);
                        tvInitial.setText(d.name != null && !d.name.isEmpty() ?
                                d.name.substring(0,1).toUpperCase() : "D");

                        setupField(findViewById(R.id.view_specialization), R.drawable.ic_operation_theatre, d.specialization);
                        setupField(findViewById(R.id.view_exp), R.drawable.ic_clock, d.exp + " Years Experience");
                        setupField(findViewById(R.id.view_fees), R.drawable.ic_landline, "Fees: Rs. " + d.appointmentFee);
                        setupField(findViewById(R.id.view_reg_no), R.drawable.ic_passkey, "Reg No: " + d.reg_no);
                    }
                });
    }

    private void setupField(android.view.View root, int icon, String value) {
        if (root != null) {
            ((TextView) root.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
            ((android.widget.ImageView) root.findViewById(R.id.icon_start)).setImageResource(icon);
        }
    }

    private void confirmRemoval() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Doctor")
                .setMessage("Are you sure you want to remove this doctor from the hospital?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Update hospital_id to null instead of deleting the whole document
                    db.collection("doctors").document(doctorId)
                            .update("hospital_id", null)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Doctor removed from hospital", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}