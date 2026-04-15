package com.example.medsync.activities.receptionist;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medsync.R;
import com.example.medsync.model.Bill;
import com.example.medsync.model.Patient;
import com.example.medsync.utils.BaseActivity;
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class DischargePatient extends BaseActivity {

    private FirebaseFirestore db;
    private String patientId, hospitalId;
    private Bill currentBill;
    private Patient patient;
    private EditText etName, etAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discharge_patient);

        db = FirebaseFirestore.getInstance();
        patientId = getIntent().getStringExtra("patient_id");
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);

        setupBaseActivityNavbar("R", "Discharge");
        setupBaseActivityFooter("home", "R");

        initUI();
        loadData();
    }

    private void initUI() {
        View cardName = findViewById(R.id.input_charge_name);
        View cardAmount = findViewById(R.id.input_charge_amount);

        // Persistent Editing State
        ViewUtils.setupEditableInfoCard(this, cardName, R.drawable.ic_edit, "Charge Name", "", v -> {});
        ViewUtils.setupEditableInfoCard(this, cardAmount, R.drawable.ic_landline, "Amount", "", v -> {});

        etName = cardName.findViewById(R.id.edit_text_input);
        etAmount = cardAmount.findViewById(R.id.edit_text_input);

        setupPersistentInput(cardName, etName, InputType.TYPE_CLASS_TEXT);
        setupPersistentInput(cardAmount, etAmount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        findViewById(R.id.btnAddCharge).setOnClickListener(v -> addNewCharge());
        findViewById(R.id.btnFinalDischarge).setOnClickListener(v -> processDischarge());
    }

    private void setupPersistentInput(View card, EditText et, int inputType) {
        ViewUtils.setInputState(this, card, "EDITING");
        et.setFocusable(true);
        et.setFocusableInTouchMode(true);
        et.setCursorVisible(true);
        et.setInputType(inputType);
        et.setText("");
        card.findViewById(R.id.container_action).setVisibility(View.GONE);
    }

    private void addNewCharge() {
        String name = etName.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (name.isEmpty() || amtStr.isEmpty() || currentBill == null) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amtStr);
            currentBill.items.put(name, amount);
            currentBill.calculateTotal();

            db.collection("hospitals").document(hospitalId).collection("bills")
                    .document(currentBill.id).set(currentBill)
                    .addOnSuccessListener(a -> {
                        etName.setText("");
                        etAmount.setText("");
                        Toast.makeText(this, "Charge Added", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        db.collection("patients").document(patientId).get().addOnSuccessListener(doc -> {
            patient = doc.toObject(Patient.class);
            fetchBill();
        });
    }

    private void fetchBill() {
        db.collection("hospitals").document(hospitalId).collection("bills")
                .whereEqualTo("patient_id", patientId)
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        currentBill = snapshots.getDocuments().get(0).toObject(Bill.class);
                        currentBill.id = snapshots.getDocuments().get(0).getId();
                        updateBillUI();
                    }
                });
    }

    private void updateBillUI() {
        View card = findViewById(R.id.bill_summary_card);
        ((TextView) card.findViewById(R.id.tvTotalAmount)).setText("Rs. " + currentBill.total_amount);
        ((TextView) card.findViewById(R.id.tvBillStatus)).setText(currentBill.status);

        LinearLayout itemsLayout = card.findViewById(R.id.layout_bill_items);
        itemsLayout.removeAllViews();
        for (Map.Entry<String, Double> entry : currentBill.items.entrySet()) {
            TextView tv = new TextView(this);
            tv.setText(entry.getKey() + ": Rs. " + entry.getValue());
            tv.setPadding(0, 8, 0, 8);
            tv.setTextColor(getResources().getColor(R.color.black));
            itemsLayout.addView(tv);
        }
    }

    private void processDischarge() {
        if (patient == null || currentBill == null) return;

        WriteBatch batch = db.batch();

        // 1. Bill update
        currentBill.status = "PAID";
        currentBill.generated_at = Timestamp.now();
        batch.set(db.collection("hospitals").document(hospitalId).collection("bills").document(currentBill.id), currentBill);

        // 2. Room update
        if (patient.room_id != null) {
            batch.update(db.collection("hospitals").document(hospitalId).collection("rooms").document(patient.room_id),
                    "isOccupied", false, "patient_id", null);
        }

        // 3. Patient status
        batch.update(db.collection("patients").document(patientId), "isAdmitted", false, "status", "DISCHARGED");

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Discharged Successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}