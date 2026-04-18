package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

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
import com.example.medsync.utils.ViewUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

// 1. Implement the Listener interface
public class PatientDetails extends BaseActivity implements TreatmentAdapter.OnTreatmentListener {

    private String patientId;
    private String hospitalId;
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

        patientId = getIntent().getStringExtra("patient_id");
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE)
                .getString("hospital_id", "");

        initViews();
        setupBaseActivityNavbar("R", "Patient Info");
        setupBaseActivityFooter("home", "R");


        if (isValidUid(patientId) && isValidUid(hospitalId)) {
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
        // 2. Pass 'this' as the second argument to the constructor
        treatmentAdapter = new TreatmentAdapter(treatmentList, this,"R");
        rvTreatments.setLayoutManager(new LinearLayoutManager(this));
        rvTreatments.setAdapter(treatmentAdapter);
    }

    // 3. Implement the onDeleteClick method
    @Override
    public void onDeleteClick(Treatment treatment) {
        if (hospitalId == null || hospitalId.isEmpty()) return;

        db.collection("hospitals").document(hospitalId)
                .collection("treatments").document(treatment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Treatment Deleted", Toast.LENGTH_SHORT).show();
                    // The SnapshotListener in ManageOperations handles updates automatically,
                    // but here we are using a .get() call, so we manually refresh:
                    loadPastTreatments();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
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
                        // Crucial: Set the IDs manually if not handled by toObjects
                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            treatmentList.get(i).setId(queryDocumentSnapshots.getDocuments().get(i).getId());
                        }
                        treatmentAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PatientDetails", "Error loading treatments: ", e);
                });
    }
    @Override
    public void onDetailsClick(Treatment treatment){
//        db.collection("hospitals").document(hospitalId).collection("treatments")
//                .document(treatment.getId())
//                .addSnapshotListener((t,e)->{
//                    if(e!=null) return;
//                    if(t==null) return;
//
//                });
    }
}