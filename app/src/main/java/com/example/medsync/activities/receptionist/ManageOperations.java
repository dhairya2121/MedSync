package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.TreatmentAdapter;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
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
        setupBaseActivityNavbar("R", "Manage Operations");
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
        types.add(TreatmentType.CHECKUP.name());
        types.add(TreatmentType.TEST.name());
        types.add(TreatmentType.EMERGENCY.name());
        types.add(TreatmentType.THERAPY.name());

        db.collection("hospitals").document(hospitalId).collection("treatments")
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
        db.collection("hospitals").document(hospitalId)
                .collection("treatments").document(treatment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show());
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