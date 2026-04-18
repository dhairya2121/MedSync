package com.example.medsync.activities.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.model.Hospital;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchHospitals extends BaseActivity {

    private RecyclerView rvHospitals;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private HospitalAdapter adapter;
    private MaterialButton btnSearch;
    private EditText etSearch;


    private List<Map<String, Object>> hospitalList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_hospitals);
        applyEdgeToEdgePadding(findViewById(R.id.main));

        // Initialize Footer
        setupBaseActivityFooter("profile", "D");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        hospitalList = new ArrayList<>();

        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search_go);
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
        MaterialButton btnAddNew = findViewById(R.id.btn_add_new);
        btnAddNew.setVisibility(View.GONE);
        updateNavbarUI();
        fetchAllHospitals();
    }


    private void updateNavbarUI() {
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserSubtext = findViewById(R.id.tvUserSubtext);
        TextView tvUserInitial = findViewById(R.id.tvUserInitial);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && tvUserName!=null && tvUserSubtext!=null && tvUserInitial!=null) {
            String name = user.getDisplayName() != null ? user.getDisplayName() : "Patient User";
            tvUserName.setText(name);
            tvUserSubtext.setText("Doctor");
            tvUserInitial.setText(name.substring(0, 1).toUpperCase());
        }
    }

    public void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        overridePendingTransition(0, 0);
        startActivity(intent);
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

            // Binding Rating Data
            Object ratingObj = data.get("rating");
            Object countObj = data.get("reviewCount");

            double rating = 0.0;
            if (ratingObj instanceof Double) rating = (Double) ratingObj;
            else if (ratingObj instanceof Long) rating = ((Long) ratingObj).doubleValue();

            long count = (countObj instanceof Long) ? (Long) countObj : 0;

            holder.rbHospital.setRating((float) rating);
            holder.tvRatingText.setText(String.format(Locale.getDefault(), "%.1f (%d)", rating, count));

            // Keep your existing OnClickListeners...

            // Inside SearchHospitals.java -> HospitalAdapter -> onBindViewHolder
            holder.itemView.setOnClickListener(v -> {
                String hospitalId = (String) data.get("hospital_id");
                if (hospitalId != null) {
                    // Link this hospital to the doctor
                    db.collection("doctors").document(mAuth.getUid())
                            .update("hospital_id", hospitalId)
                            .addOnSuccessListener(aVoid -> {
                                // The SnapshotListener in Dashboard will automatically refresh the UI
                                finish();
                            });
                }
            });
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

}