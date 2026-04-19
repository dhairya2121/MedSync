package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.TreatmentAdapter;
import com.example.medsync.model.BookedSlot;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ManageOperations extends BaseActivity implements TreatmentAdapter.OnTreatmentListener {

    private RecyclerView recyclerView;
    private TreatmentAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_operations);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home", "R");

        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);
        if (hospitalId == null) {
            Toast.makeText(this, "Link a hospital first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvOperations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Pass 'this' as the listener for the delete button
        adapter = new TreatmentAdapter(new ArrayList<>(), this,"R");
        recyclerView.setAdapter(adapter);



        fetchOperations();
    }

    private void fetchOperations() {
        List<String> types = new ArrayList<>();
        types.add(TreatmentType.OPERATION.name());
        types.add(TreatmentType.TEST.name());
        types.add(TreatmentType.EMERGENCY.name());
        types.add(TreatmentType.THERAPY.name());

        db.collection("hospitals").document(hospitalId)
                .collection("treatments")
                .whereIn("type", types)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    List<Treatment> list = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Treatment t = doc.toObject(Treatment.class);
                            t.setId(doc.getId());
                            list.add(t);
                        }
                    }
                    adapter.setList(list);
                });
    }

    // DELETE LOGIC:
    // In ManageOperations.java
    @Override
    public void onDeleteClick(Treatment treatment) {
        if (treatment.start == null || treatment.end == null) {
            Toast.makeText(this, "Cannot delete: Time data missing", Toast.LENGTH_SHORT).show();
            return;
        }

        List<BookedSlot> slotsToRemove = new ArrayList<>();
        String tId = treatment.getId();

        // 1. Generate all 30-minute slots between start and end
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(treatment.start.toDate());

        // While start time is before end time
        while (cal.getTime().before(treatment.end.toDate())) {
            Timestamp currentSlotStart = new Timestamp(cal.getTime());
            slotsToRemove.add(new BookedSlot(currentSlotStart, tId));

            // Move to next 30-min slot
            cal.add(java.util.Calendar.MINUTE, 30);
        }

        // 2. Remove slots from Doctor's array
        // Note: Use .toArray() or pass elements if arrayRemove doesn't accept the List directly in your SDK version
        db.collection("doctors").document(treatment.examiner_id)
                .update("booked_slots", FieldValue.arrayRemove(slotsToRemove.toArray()))
                .addOnSuccessListener(d -> {

                    // 3. Delete the Treatment document
                    db.collection("hospitals").document(hospitalId)
                            .collection("treatments").document(tId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                // 4. Check if patient has any other records in this hospital
                                // If not, unlink the hospital from the patient profile
                                db.collection("hospitals").document(hospitalId)
                                        .collection("treatments")
                                        .whereEqualTo("patient_id", treatment.patient_id)
                                        .get().addOnSuccessListener(treatmentList -> {
                                            if (treatmentList == null || treatmentList.isEmpty()) {
                                                db.collection("patients").document(treatment.patient_id)
                                                        .update("hospital_id", "")
                                                        .addOnSuccessListener(p -> {
                                                            Toast.makeText(this, "Operation Deleted & Patient Unlinked", Toast.LENGTH_SHORT).show();
                                                        });
                                            } else {
                                                Toast.makeText(this, "Operation Deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete treatment", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to clear doctor slots", Toast.LENGTH_SHORT).show());
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