package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.AppointmentAdapter;
import com.example.medsync.model.BookedSlot;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageAppointments extends BaseActivity implements AppointmentAdapter.OnAppointmentListener {

    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_appointments);

        // 1. Setup UI components from BaseActivity
        // Apply padding to the root layout ID 'main' to handle status and nav bars correctly
        applyEdgeToEdgePadding(findViewById(R.id.main)); 
        setupBaseActivityFooter("home", "R");

        // 2. Get Hospital ID
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);

        if (hospitalId == null) {
            Toast.makeText(this, "Hospital ID not found. Please link a hospital first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // 3. Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewAppointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter(this);
        recyclerView.setAdapter(adapter);

        fetchTreatments();
    }

    private void fetchTreatments() {
        List<String> appointmentTypes = Arrays.asList(
                TreatmentType.APPOINTMENT.name(),
                TreatmentType.FOLLOW_UP.name(),
                TreatmentType.CHECKUP.name()
        );

        db.collection("hospitals").document(hospitalId).collection("treatments")
                .whereIn("type", appointmentTypes)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<Treatment> validAppointments = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : value) {
                        Treatment t = doc.toObject(Treatment.class);
                        t.setId(doc.getId());

                        // Directly check the patient document
                        db.collection("patients").document(t.patient_id).get()
                                .addOnSuccessListener(pDoc -> {
                                    Boolean isAdmitted = pDoc.getBoolean("isAdmitted");
                                    // If not admitted, add to list and refresh UI
                                    if (isAdmitted == null || !isAdmitted) {
                                        validAppointments.add(t);
                                        adapter.setTreatments(new ArrayList<>(validAppointments));
                                    }
                                });
                    }
                });
    }


    @Override
    public void onAdmitClick(Treatment treatment) {
        // Redirect to AdmitPatient activity
        Intent intent = new Intent(this, AdmitPatient.class);

        // Required by AdmitPatient.java: patient_id and patient_name
        intent.putExtra("patient_id", treatment.patient_id);
        intent.putExtra("patient_name", treatment.patient_name);

        // Pre-pass doctor info for better UX
        intent.putExtra("doctor_id", treatment.examiner_id);
        intent.putExtra("doctor_name", treatment.examiner_name);

        startActivity(intent);
    }
    @Override
    public void onDeleteClick(Treatment treatment) {
        BookedSlot b=new BookedSlot(treatment.start,treatment.getId());
        db.collection("doctors").document(treatment.examiner_id)
                        .update("booked_slots", FieldValue.arrayRemove(b))
                                .addOnSuccessListener(d->{
                                    db.collection("hospitals").document(hospitalId)
                                            .collection("treatments").document(treatment.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                db.collection("hospitals").document(hospitalId)
                                                                .collection("treatments")
                                                                        .whereEqualTo("patient_id",treatment.patient_id)
                                                                                .get().addOnSuccessListener(treatmentList->{
                                                                                    if(treatmentList==null || treatmentList.isEmpty()){
                                                                                        db.collection("patients").document(treatment.patient_id).update("hospital_id","")
                                                                                                .addOnSuccessListener(p->{
                                                                                                    Toast.makeText(this, "Appointment Deleted", Toast.LENGTH_SHORT).show();
                                                                                                });
                                                                                    }
                                                        });
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());
                                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show());


    }
}