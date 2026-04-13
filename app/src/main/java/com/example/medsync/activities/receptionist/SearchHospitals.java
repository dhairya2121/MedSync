package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchHospitals extends BaseActivity {

    private EditText etSearch;
    private MaterialButton btnSearch, btnAddNew;
    private RecyclerView rvHospitals;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private HospitalAdapter adapter;
    private List<Map<String, Object>> hospitalList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_hospitals);
        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityFooter("rolebased", "R");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        hospitalList = new ArrayList<>();

        etSearch = findViewById(R.id.et_search_hospitals);
        btnSearch = findViewById(R.id.btn_search_go);
        btnAddNew = findViewById(R.id.btn_add_new);
        rvHospitals = findViewById(R.id.rv_hospitals);

        adapter = new HospitalAdapter(hospitalList);
        rvHospitals.setLayoutManager(new LinearLayoutManager(this));
        rvHospitals.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchHospitals(query);
            } else {
                fetchAllHospitals();
            }
        });

        // ✅ Handle "Add New" Click
        btnAddNew.setOnClickListener(v -> createNewHospitalAndLink());

        fetchAllHospitals();
    }

    private void createNewHospitalAndLink() {
        // 1. Create Dummy Hospital Data
        Map<String, Object> dummyHospital = new HashMap<>();
        dummyHospital.put("legal_name", "New Hospital Name");
        dummyHospital.put("address", "Enter Address");
        dummyHospital.put("phone", "");
        dummyHospital.put("email", "");
        dummyHospital.put("rating", 0.0);
        dummyHospital.put("reviewCount", 0);

        // 2. Add to "hospitals" collection
        db.collection("hospitals")
                .add(dummyHospital)
                .addOnSuccessListener(documentReference -> {
                    String newHospitalId = documentReference.getId();
                    // 3. Link to current Receptionist
                    linkHospitalToReceptionist(newHospitalId);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to create hospital", Toast.LENGTH_SHORT).show());
    }

    private void linkHospitalToReceptionist(String hospitalId) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("receptionists").document(uid)
                .update("hospital_id", hospitalId)
                .addOnSuccessListener(aVoid -> {
                    // 4. Navigate to Hospital Activity
                    Intent intent = new Intent(this, Hospital.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to link hospital", Toast.LENGTH_SHORT).show());
    }

    private class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {
        private final List<Map<String, Object>> hospitals;

        public HospitalAdapter(List<Map<String, Object>> hospitals) {
            this.hospitals = hospitals;
        }

        public void updateData(List<Map<String, Object>> newData) {
            hospitals.clear();
            hospitals.addAll(newData);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hospital_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Map<String, Object> data = hospitals.get(position);

            holder.tvName.setText((String) data.getOrDefault("legal_name", "Unknown Hospital"));
            holder.tvLocation.setText((String) data.getOrDefault("address", "No address provided"));

            // ✅ Handle existing item click
            holder.itemView.setOnClickListener(v -> {
                String hospitalId = (String) data.get("hospital_id");
                if (hospitalId != null) {
                    linkHospitalToReceptionist(hospitalId);
                }
            });
        }

        @Override
        public int getItemCount() { return hospitals.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLocation;
            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_hospital_name);
                tvLocation = itemView.findViewById(R.id.tv_location);
            }
        }
    }

    private void fetchAllHospitals() {
        db.collection("hospitals").limit(20).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("hospital_id", doc.getId());
                        results.add(data);
                    }
                    updateUI(results);
                });
    }

    private void searchHospitals(String searchText) {
        db.collection("hospitals")
                .orderBy("legal_name")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("hospital_id", doc.getId());
                        results.add(data);
                    }
                    updateUI(results);
                });
    }

    private void updateUI(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            Toast.makeText(this, "No hospitals found", Toast.LENGTH_SHORT).show();
        }
        adapter.updateData(results);
    }
}