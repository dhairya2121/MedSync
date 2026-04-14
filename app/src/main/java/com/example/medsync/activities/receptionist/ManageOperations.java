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

public class ManageOperations extends BaseActivity {

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

        // Fix: Pass empty list to constructor
        adapter = new TreatmentAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        MaterialButton btnAddNew = findViewById(R.id.btn_add_new);
        btnAddNew.setOnClickListener(v -> {
            // Intent intent = new Intent(this, AddOperationActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Redirecting to Schedule Operation...", Toast.LENGTH_SHORT).show();
        });

        fetchOperations();
    }

    private void fetchOperations() {
        // Create a list of types to exclude (currently just APPOINTMENT)
        List<String> types = new ArrayList<>();
        types.add(TreatmentType.OPERATION.name());
        types.add(TreatmentType.CHECKUP.name());
        types.add(TreatmentType.TEST.name());
        types.add(TreatmentType.EMERGENCY.name());
        types.add(TreatmentType.THERAPY.name());

        db.collection("hospitals").document(hospitalId).collection("treatments")
                .whereIn("type", types)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Treatment> list = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Treatment t = doc.toObject(Treatment.class);
                            t.setId(doc.getId());
                            list.add(t);
                        }
                    }
                    // Update the adapter with the filtered list
                    adapter.setList(list);
                });
    }
}