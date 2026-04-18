package com.example.medsync.activities.patient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Report;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TreatmentDetails extends BaseActivity {

    private String treatmentId, hospitalId;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treatment_details);

        db = FirebaseFirestore.getInstance();
        treatmentId = getIntent().getStringExtra("treatment_id");
        hospitalId = getIntent().getStringExtra("hospital_id");

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Record Details");
        setupBaseActivityFooter("home", "P");

        if (treatmentId != null && hospitalId != null) {
            fetchTreatmentDetails();
        } else {
            Toast.makeText(this, "Error: Data missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchTreatmentDetails() {
        db.collection("hospitals").document(hospitalId)
                .collection("treatments").document(treatmentId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;

                    Treatment t = doc.toObject(Treatment.class);
                    t.setId(treatmentId);
                    if (t != null) {
                        updateUI(t);
                    }
                });
    }

    private void updateUI(Treatment t) {
        // 1. General Info
        ((TextView) findViewById(R.id.tvType)).setText(t.type);
        ((TextView) findViewById(R.id.tvDoctorName)).setText("Examined by " + t.getDoctorName());
        ((TextView) findViewById(R.id.tvStatus)).setText(t.status);

        if (t.getTimestamp() != null) {
            ((TextView) findViewById(R.id.tvDateTime)).setText(dateFormat.format(t.getTimestamp().toDate()));
        }

        // 2. Report Handling
        if (t.report != null) {
            findViewById(R.id.labelReport).setVisibility(View.VISIBLE);
            findViewById(R.id.cardReport).setVisibility(View.VISIBLE);

            Report r = t.report;
            ((TextView) findViewById(R.id.tvDiagnosis)).setText(nullableText(r.diagnosis));
            ((TextView) findViewById(R.id.tvPrescription)).setText(nullableText(r.prescription));
            ((TextView) findViewById(R.id.tvObservations)).setText(nullableText(r.observations));
        }


        fetchAndPopulateBill(t);
    }

    private void fetchAndPopulateBill(Treatment t) {
        View billContainer = findViewById(R.id.bill_card_container);
        MaterialButton btnPayBill=findViewById(R.id.btnPayBill);
        btnPayBill.setOnClickListener(v->{
            Intent intent =new Intent(this, Billing.class);
            intent.putExtra("treatment_id", t.getId());
            intent.putExtra("hospital_id", hospitalId);
            startActivity(intent);
        });
        // Checking if bill exists (Checking a hypothetical field or subcollection)
        // Reusing your item_bill_card.xml IDs
        db.collection("hospitals").document(hospitalId)
                .collection("bills").document(treatmentId)
                .addSnapshotListener((doc,e)-> {
                    if(e!=null || doc==null){
                        return;
                    }
                    if (doc!=null && doc.exists()) {
                        findViewById(R.id.labelBill).setVisibility(View.VISIBLE);
                        billContainer.setVisibility(View.VISIBLE);

                        // Example: Set total from the first document in bill subcollection
                        Double total = doc.getDouble("total_amount");
                        String status = doc.getString("status");

                        ((TextView) billContainer.findViewById(R.id.tvTotalAmount)).setText("Rs. " + (total != null ? total : "0.0"));
                        ((TextView) billContainer.findViewById(R.id.tvBillStatus)).setText(status != null ? status : "UNPAID");
                        if(t.status.equalsIgnoreCase(TreatmentStatus.SUCCESS.name()) && status.equalsIgnoreCase("PENDING")){
                            btnPayBill.setVisibility(View.VISIBLE);
                        }
                        else{
                            btnPayBill.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private String nullableText(String input) {
        return (input == null || input.isEmpty()) ? "Not provided" : input;
    }
}