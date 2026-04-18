package com.example.medsync.activities.patient;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class Treatments extends BaseActivity implements TreatmentAdapter.OnTreatmentListener {

    private RecyclerView recyclerView;
    private TreatmentAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_operations);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Patient");
        setupBaseActivityFooter("home", "P");

        hospitalId = getIntent().getStringExtra("hospital_id");
        if (hospitalId == null) {
            Toast.makeText(this, "No Recent Treatments", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvOperations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Pass 'this' as the listener for the delete button
        adapter = new TreatmentAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);



        fetchOperations();
    }

    private void fetchOperations() {
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();

        List<String> types = new ArrayList<>();
        types.add(TreatmentType.OPERATION.name());
        types.add(TreatmentType.CHECKUP.name());
        types.add(TreatmentType.TEST.name());
        types.add(TreatmentType.EMERGENCY.name());
        types.add(TreatmentType.THERAPY.name());

        db.collection("hospitals").document(hospitalId).collection("treatments")
                .whereEqualTo("patient_id",user.getUid().toString())
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

    @Override
    public void onDeleteClick(Treatment treatment) {
        Toast.makeText(this,"Not Allowed",Toast.LENGTH_SHORT).show();
    }
}