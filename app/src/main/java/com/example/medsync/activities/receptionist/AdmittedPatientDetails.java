package com.example.medsync.activities.receptionist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Bill;
import com.example.medsync.model.Patient;
import com.example.medsync.model.Treatment;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class AdmittedPatientDetails extends BaseActivity {

    private String patientId,patientName,doctorName;
    private FirebaseFirestore db;
    private TextView tvInitial, tvFullName;
    private View billCard;
    private String hospitalId;
    private LinearLayout llTreatmentsContainer;
    private TextView tvTreatmentsTitle;
    private MaterialButton btnDischarge,btnScheduleOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admitted_patient_details);

        db = FirebaseFirestore.getInstance();
        patientId = getIntent().getStringExtra("patient_id");
        hospitalId = getSharedPreferences("medsync_prefs",Context.MODE_PRIVATE).getString("hospital_id", null);

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
        billCard = findViewById(R.id.bill_card_container);
        llTreatmentsContainer = findViewById(R.id.llTreatmentsContainer);
        tvTreatmentsTitle = findViewById(R.id.tvTreatmentsTitle);
        btnDischarge.setOnClickListener(v -> {
            // Redirect to future discharge activity
             Intent intent = new Intent(this, DischargePatient.class);
             intent.putExtra("patient_id", patientId);
             startActivity(intent);
             finish();
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
                        if(p.doctor_id != null) {
                            db.collection("doctors").document(p.doctor_id).addSnapshotListener((doctorDoc, err) -> {
                                if (err != null || doctorDoc == null || !doctorDoc.exists()) {
                                    doctorName = "N/A";
                                } else {
                                    doctorName = doctorDoc.getString("name");
                                    // If the doctor name changes, we don't need to do anything
                                    // because doctorName is only used when clicking "Schedule Operation"
                                }
                            });
                        }

                        // Reusing the setupField logic from your PatientDetails activity
                        setupField(findViewById(R.id.roomNo), "Room " + p.room_no);
                        setupField(findViewById(R.id.admittedDate), "Admitted: " + p.admittedOn);
                        setupField(findViewById(R.id.gender), p.gender);
                        setupField(findViewById(R.id.age), p.age + " yrs");
                        fetchPatientTreatments();
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


    // Inside fetchPatientTreatments()
    private void fetchPatientTreatments() {
        db.collection("hospitals").document(hospitalId)
                .collection("treatments")
                .whereEqualTo("patient_id", patientId)
                .whereEqualTo("type", "MEDICATION") // Only Medication visible to Receptionist
                .addSnapshotListener((snapshots, e) -> {
                    llTreatmentsContainer.removeAllViews();
                    if (snapshots == null || snapshots.isEmpty()) {
                        tvTreatmentsTitle.setVisibility(View.GONE);
                        return;
                    }
                    tvTreatmentsTitle.setVisibility(View.VISIBLE);
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        Treatment t = doc.toObject(Treatment.class);
                        if (t != null) addTreatmentCard(t);
                    }
                });
    }

    private void addTreatmentCard(Treatment t) {
        // Inflate the treatment card layout
        View card = getLayoutInflater().inflate(R.layout.item_treatment_card, llTreatmentsContainer, false);

        // 1. Format Date to DD-MM-YYYY
        String dateStr = "N/A";
        if (t.getTimestamp() != null) {
            // Use SimpleDateFormat for the requested format
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            dateStr = sdf.format(t.getTimestamp().toDate());
        }

        // 2. Set Basic Info
        ((TextView) card.findViewById(R.id.tvTreatmentType)).setText(t.type);
        ((TextView) card.findViewById(R.id.tvTreatmentDate)).setText(dateStr);

        // Safety check for doctor name
        String docName = (t.examiner_name != null) ? t.examiner_name : "N/A";
        ((TextView) card.findViewById(R.id.tvDoctorName)).setText("Dr. " + docName);

        // 3. Hide delete button if it exists (receptionist shouldn't delete medication here)
        View btnDelete = card.findViewById(R.id.btnDelete);
        if (btnDelete != null) btnDelete.setVisibility(View.GONE);

        // 4. Update Status Badge (Optional but good for UI consistency)
        TextView tvStatus = card.findViewById(R.id.tvStatusBadge);
        if (tvStatus != null) {
            tvStatus.setText(t.status != null ? t.status : "ONGOING");
        }

        llTreatmentsContainer.addView(card);
    }

    // Update fetchPatientBill to add logging for debugging
    private void fetchPatientBill() {
        if (hospitalId == null || patientId == null) {
            android.util.Log.e("BILL_DEBUG", "Hospital or Patient ID is null");
            return;
        }

        db.collection("hospitals").document(hospitalId)
                .collection("bills")
                .whereEqualTo("patient_id", patientId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        android.util.Log.e("BILL_DEBUG", "Error fetching bill", e);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        // If no bill exists, maybe show a "No Bill" message or keep it hidden
                        android.util.Log.d("BILL_DEBUG", "No bill found for patient: " + patientId);
                        findViewById(R.id.tvBillTitle).setVisibility(View.GONE);
                        billCard.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("BILL_DEBUG", "Bill found!");
                        Bill bill = snapshots.getDocuments().get(0).toObject(Bill.class);
                        if (bill != null) {
                            displayBill(bill);
                        }
                    }
                });
    }

    private void displayBill(Bill bill) {
        findViewById(R.id.tvBillTitle).setVisibility(View.VISIBLE);
        billCard.setVisibility(View.VISIBLE);

        TextView tvTotal = billCard.findViewById(R.id.tvTotalAmount);
        TextView tvStatus = billCard.findViewById(R.id.tvBillStatus);
        LinearLayout itemsLayout = billCard.findViewById(R.id.layout_bill_items);

        tvTotal.setText(String.format("$ %.2f", bill.total_amount));
        tvStatus.setText(bill.status);

        // Clear and fill items
        itemsLayout.removeAllViews();
        for (Map.Entry<String, Double> entry : bill.items.entrySet()) {
            TextView tvItem = new TextView(this);
            tvItem.setText(entry.getKey() + ": $ " + entry.getValue());
            tvItem.setPadding(0, 4, 0, 4);
            tvItem.setTextColor(getColor(R.color.black));
            itemsLayout.addView(tvItem);
        }
    }
}