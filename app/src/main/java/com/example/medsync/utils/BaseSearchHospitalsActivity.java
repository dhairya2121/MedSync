package com.example.medsync.utils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseSearchHospitalsActivity extends BaseActivity {

    protected RecyclerView rvHospitals;
    protected FirebaseFirestore db;
    protected FirebaseAuth mAuth;
    protected HospitalAdapter adapter;
    protected EditText etSearch;
    protected List<Map<String, Object>> hospitalList;

    // To be implemented by specific roles
    protected abstract String getRoleCode(); // "R", "D", "P", etc.
    protected abstract void onHospitalClick(String hospitalId, Map<String, Object> data);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_hospitals);
        applyEdgeToEdgePadding(findViewById(R.id.main));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        hospitalList = new ArrayList<>();

//        setupBaseActivityNavbar(getRoleCode(), getRoleTitle());
        setupBaseActivityFooter("rolebased", getRoleCode());

        initCommonUI();
        fetchAllHospitals();
    }

    private void initCommonUI() {
        etSearch = findViewById(R.id.et_search);
        rvHospitals = findViewById(R.id.rv_hospitals);
        adapter = new HospitalAdapter(hospitalList);

        rvHospitals.setLayoutManager(new LinearLayoutManager(this));
        rvHospitals.setAdapter(adapter);

        findViewById(R.id.btn_search_go).setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) searchHospitals(query);
            else fetchAllHospitals();
        });

        // "Add New" is hidden by default. Receptionist will override this.
        findViewById(R.id.btn_add_new).setVisibility(View.GONE);
    }

    protected void fetchAllHospitals() {
        db.collection("hospitals").limit(20).get().addOnSuccessListener(snapshots -> {
            List<Map<String, Object>> results = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                Map<String, Object> data = doc.getData();
                data.put("hospital_id", doc.getId());
                results.add(data);
            }
            updateUIList(results);
        });
    }

    protected void searchHospitals(String searchText) {
        db.collection("hospitals").orderBy("legal_name")
                .startAt(searchText).endAt(searchText + "\uf8ff").get()
                .addOnSuccessListener(snapshots -> {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("hospital_id", doc.getId());
                        results.add(data);
                    }
                    updateUIList(results);
                });
    }

    protected void updateUIList(List<Map<String, Object>> results) {
        hospitalList.clear();
        hospitalList.addAll(results);
        adapter.notifyDataSetChanged();
        if (results.isEmpty()) Toast.makeText(this, "No hospitals found", Toast.LENGTH_SHORT).show();
    }

    protected class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {
        private final List<Map<String, Object>> hospitals;
        public HospitalAdapter(List<Map<String, Object>> hospitals) { this.hospitals = hospitals; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hospital_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> data = hospitals.get(position);
            holder.tvName.setText((String) data.getOrDefault("legal_name", "Hospital"));
            holder.tvLocation.setText((String) data.getOrDefault("address", "No address provided"));

            // Binding Rating Data
            Object ratingObj = data.get("rating");
            double rating = (ratingObj instanceof Double) ? (Double) ratingObj : (ratingObj instanceof Long) ? ((Long) ratingObj).doubleValue() : 0.0;
            long count = (data.get("reviewCount") instanceof Long) ? (Long) data.get("reviewCount") : 0;

            holder.rbHospital.setRating((float) rating);
            holder.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f (%d)", rating, count));

            holder.itemView.setOnClickListener(v -> onHospitalClick((String) data.get("hospital_id"), data));
        }

        @Override
        public int getItemCount() { return hospitals.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLocation, tvRatingText;
            RatingBar rbHospital;
            public ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_hospital_name);
                tvLocation = itemView.findViewById(R.id.tv_location);
                tvRatingText = itemView.findViewById(R.id.tv_rating_text);
                rbHospital = itemView.findViewById(R.id.rb_hospital);
            }
        }
    }
}