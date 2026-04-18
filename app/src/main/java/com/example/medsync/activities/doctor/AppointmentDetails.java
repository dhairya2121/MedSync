package com.example.medsync.activities.doctor;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.medsync.R;
import com.example.medsync.model.BookedSlot;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Report;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.Supplements;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AppointmentDetails extends BaseActivity {

    private FirebaseFirestore db;
    private String treatmentId, hospitalId, patientId;
    private Treatment treatment;
    private Report currentReport = new Report();
    private List<String> selectedSupplements = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_details);

        db = FirebaseFirestore.getInstance();
        treatmentId = getIntent().getStringExtra("treatment_id");
        hospitalId = getIntent().getStringExtra("hospital_id");
        patientId = getIntent().getStringExtra("patient_id");

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("D", "Consultation");
        setupBaseActivityFooter("home", "D");

        loadData();

        findViewById(R.id.btn_done).setOnClickListener(v -> completeAppointment());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> closeWithStatus(TreatmentStatus.FAILED));
    }

    private void loadData() {
        db.collection("patients").document(patientId).get().addOnSuccessListener(doc -> {
            Patient p = doc.toObject(Patient.class);
            if (p != null) {
                ((TextView) findViewById(R.id.tvPatientName)).setText(p.name);
                ((TextView) findViewById(R.id.tvPatientBio)).setText(p.gender + ", " + p.age + " yrs");
                ((TextView) findViewById(R.id.tvProfileInitial)).setText(p.name.substring(0, 1).toUpperCase());
            }
        });

        db.collection("hospitals").document(hospitalId).collection("treatments").document(treatmentId).get()
                .addOnSuccessListener(doc -> {
                    treatment = doc.toObject(Treatment.class);
                    setupReportEditors();
                    setupSupplementsMultiSelect();
                });
    }

    private void setupReportEditors() {
        setupReportCard(findViewById(R.id.input_diagnosis), "Diagnosis", "Enter Diagnosis", "diagnosis");
        setupReportCard(findViewById(R.id.input_observations), "Observations", "General Observations", "observations");
        setupReportCard(findViewById(R.id.input_prescription), "Prescription", "Medicine & Dosage", "prescription");
    }

    private void setupSupplementsMultiSelect() {
        View card = findViewById(R.id.input_supplements);
        ViewUtils.setupEditableInfoCard(this, card, R.drawable.ic_heart_care, "Supplements", "Select Items", null);

        // Override edit button to show Multi-Choice Dialog
        card.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Supplements[] allSupps = Supplements.values();
            String[] items = new String[allSupps.length];
            boolean[] checkedItems = new boolean[allSupps.length];

            for (int i = 0; i < allSupps.length; i++) {
                items[i] = allSupps[i].getDisplayName();
                checkedItems[i] = selectedSupplements.contains(items[i]);
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Supplements")
                    .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                        if (isChecked) selectedSupplements.add(items[which]);
                        else selectedSupplements.remove(items[which]);
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        String display = selectedSupplements.isEmpty() ? "None Selected" : String.join(", ", selectedSupplements);
                        ((EditText) card.findViewById(R.id.edit_text_input)).setText(display);
                    })
                    .show();
        });
    }

    private void setupReportCard(View card, String label, String hint, String field) {
        ViewUtils.setupEditableInfoCard(this, card, R.drawable.ic_edit, label, hint, newVal -> {
            switch (field) {
                case "diagnosis": currentReport.diagnosis = newVal; break;
                case "observations": currentReport.observations = newVal; break;
                case "prescription": currentReport.prescription = newVal; break;
            }
            ViewUtils.setInputState(this, card, "IDLE");
        });
    }

    private void completeAppointment() {
        if (currentReport.diagnosis == null || currentReport.diagnosis.isEmpty()) {
            Toast.makeText(this, "Please enter a diagnosis", Toast.LENGTH_SHORT).show();
            return;
        }
        closeWithStatus(TreatmentStatus.SUCCESS);
    }

    private void closeWithStatus(TreatmentStatus status) {
        String doctorUid = FirebaseAuth.getInstance().getUid();

        db.collection("hospitals").document(hospitalId).collection("treatments").document(treatmentId)
                .update(
                        "status", status.name(),
                        "report", currentReport,
                        "supplements", selectedSupplements
                )
                .addOnSuccessListener(a -> {
                    if (treatment != null && treatment.getTimestamp() != null) {
                        BookedSlot slotToRemove = new BookedSlot(treatment.getTimestamp(), treatmentId);
                        db.collection("doctors").document(doctorUid)
                                .update("booked_slots", FieldValue.arrayRemove(slotToRemove))
                                .addOnSuccessListener(b -> {
                                    Toast.makeText(this, "Consultation Completed", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    } else {
                        finish();
                    }
                });
    }
}