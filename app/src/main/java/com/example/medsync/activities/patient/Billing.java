package com.example.medsync.activities.patient;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Bill;
import com.example.medsync.model.Treatment;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class Billing extends BaseActivity {

    private FirebaseFirestore db;
    private String treatmentId, hospitalId;
    private Bill currentBill;
    private RatingBar ratingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);

        db = FirebaseFirestore.getInstance();
        treatmentId = getIntent().getStringExtra("treatment_id");
        hospitalId = getIntent().getStringExtra("hospital_id");

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Check-out");
        setupBaseActivityFooter("home", "P");

        ratingBar = findViewById(R.id.rating_bar_patient);

        if (treatmentId != null && hospitalId != null) {
            loadBillAndTreatmentData();
        }

        findViewById(R.id.btnConfirmPay).setOnClickListener(v -> processPatientConfirmation());
    }

    private void loadBillAndTreatmentData() {
        // 1. Load Bill (Path: hospitals/ID/bills/treatmentID)
        db.collection("hospitals").document(hospitalId)
                .collection("bills").document(treatmentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentBill = doc.toObject(Bill.class);
                        currentBill.id = doc.getId();
                        updateBillUI();
                    } else {
                        Toast.makeText(this, "Bill not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        // 2. Load Treatment Metadata for the summary fields
        db.collection("hospitals").document(hospitalId)
                .collection("treatments").document(treatmentId).get()
                .addOnSuccessListener(doc -> {
                    Treatment t = doc.toObject(Treatment.class);
                    if (t != null) {
                        setupViewField(findViewById(R.id.view_doctor), R.drawable.ic_filled_user, "Dr. " + t.getDoctorName());
                        setupViewField(findViewById(R.id.view_treatment_type), R.drawable.ic_stethoscope, t.type);
                    }
                });
    }

    private void updateBillUI() {
        View card = findViewById(R.id.bill_summary_card);
        ((TextView) card.findViewById(R.id.tvTotalAmount)).setText("Rs. " + currentBill.total_amount);
        ((TextView) card.findViewById(R.id.tvBillStatus)).setText(currentBill.status);

        LinearLayout itemsLayout = card.findViewById(R.id.layout_bill_items);
        itemsLayout.removeAllViews();

        // Loop through itemized charges
        if (currentBill.items != null) {
            for (Map.Entry<String, Double> entry : currentBill.items.entrySet()) {
                View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                TextView text1 = itemView.findViewById(android.R.id.text1);
                TextView text2 = itemView.findViewById(android.R.id.text2);

                text1.setText(entry.getKey());
                text1.setTextColor(getColor(R.color.black));
                text2.setText("Rs. " + entry.getValue());
                text2.setTextColor(getColor(R.color.hard_grey));

                itemsLayout.addView(itemView);
            }
        }
    }

    private void setupViewField(View root, int icon, String value) {
        if (root == null) return;
        ((TextView) root.findViewById(R.id.tv_field)).setText(value);
        ((ImageView) root.findViewById(R.id.icon_start)).setImageResource(icon);
    }

    private void processPatientConfirmation() {        if (currentBill == null) return;

        float patientRating = ratingBar.getRating();
        if (patientRating == 0) {
            Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use a Transaction to ensure the Average Rating calculation is accurate
        db.runTransaction(transaction -> {
            // 1. References
            DocumentReference hospitalRef =
                    db.collection("hospitals").document(hospitalId);
            DocumentReference billRef =
                    db.collection("hospitals").document(hospitalId)
                            .collection("bills").document(currentBill.id);

            // 2. Read current Hospital data
            DocumentSnapshot hospitalDoc = transaction.get(hospitalRef);

            double oldRating = 0;
            long oldCount = 0;

            if (hospitalDoc.exists()) {
                Double r = hospitalDoc.getDouble("rating");
                Long c = hospitalDoc.getLong("reviewCount");
                if (r != null) oldRating = r;
                if (c != null) oldCount = c;
            }

            // 3. Calculate new average: ( (Average * Count) + NewRating ) / (Count + 1)
            long newCount = oldCount + 1;
            double newAverage = ((oldRating * oldCount) + patientRating) / newCount;

            // 4. Perform Updates (These happen together atomically)
            transaction.update(hospitalRef,
                    "rating", newAverage,
                    "reviewCount", newCount
            );

            transaction.update(billRef,
                    "patient_rating", patientRating,
                    "payment_initiated", true,
                    "status", "UNDER VERIFICATION"
            );

            return null; // Transaction completed
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Review submitted! Receptionist will verify payment.", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            android.util.Log.e("Billing", "Transaction failed: ", e);
            Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
        });
    }

}