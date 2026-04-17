package com.example.medsync.activities.patient;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;import androidx.appcompat.app.AlertDialog;
import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
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

        setupBaseActivityNavbar("P", "Patient");
        setupBaseActivityFooter("home", "P");

        tvInitial = findViewById(R.id.tvProfileInitial);
        tvFullName = findViewById(R.id.tvDoctorNameLarge);

        if (doctorId != null) {
            loadDoctorData();
        }

        // Correct way to find and hide the button
        MaterialButton deleteBtn = findViewById(R.id.btn_remove_doctor);

        if (deleteBtn != null) {
            // Standard Android visibility constant
            deleteBtn.setVisibility(View.GONE);
        }
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
                        setupField(findViewById(R.id.view_reg_no), R.drawable.ic_contact_book, "Reg No: " + d.reg_no);
                    }
                });
    }

    private void setupField(android.view.View root, int icon, String value) {
        if (root != null) {
            ((TextView) root.findViewById(R.id.tv_field)).setText(value != null ? value : "N/A");
            ((android.widget.ImageView) root.findViewById(R.id.icon_start)).setImageResource(icon);
        }
    }

}