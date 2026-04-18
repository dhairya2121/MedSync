package com.example.medsync.activities.patient;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.TreatmentAdapter;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class Treatments extends BaseActivity implements TreatmentAdapter.OnTreatmentListener {

    private RecyclerView recyclerView;
    private TreatmentAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    private String selectedFilter = "all"; // "all", "ongoing", "upcoming", "completed"
    private List<Treatment> fullList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_operations); // Reusing this layout

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("P", "Treatments");
        setupBaseActivityFooter("home", "P");

        hospitalId = getIntent().getStringExtra("hospital_id");
        if (hospitalId == null) {
            Toast.makeText(this, "Hospital ID Missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvOperations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TreatmentAdapter(new ArrayList<>(), this, "P");
        recyclerView.setAdapter(adapter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setTitle("My Treatments");

        setupFilterChips();
        fetchTreatmentsFromDb();
    }

    private void setupFilterChips() {
        TextView tvOngoing = findViewById(R.id.tvOngoingFilter);
        TextView tvUpcoming = findViewById(R.id.tvUpcomingFilter);
        TextView tvCompleted = findViewById(R.id.tvCompletedFilter);

        tvOngoing.setOnClickListener(v -> handleFilterClick(tvOngoing, "ongoing"));
        tvUpcoming.setOnClickListener(v -> handleFilterClick(tvUpcoming, "upcoming"));
        tvCompleted.setOnClickListener(v -> handleFilterClick(tvCompleted, "completed"));
    }

    private void handleFilterClick(TextView clickedTv, String status) {
        // If clicking the same active filter, toggle it OFF
        if (selectedFilter.equals(status)) {
            selectedFilter = "all";
            resetAllChips();
        } else {
            selectedFilter = status;
            resetAllChips();
            setActiveStyle(clickedTv);
        }
        applyFilterAndRefreshList();
    }

    private void setActiveStyle(TextView tv) {
        tv.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.soft_green)));
        tv.setTextColor(getColor(R.color.hard_green));
    }

    private void resetAllChips() {
        int softGrey = getColor(R.color.soft_grey);
        int hardGrey = getColor(R.color.hard_grey);

        TextView[] chips = {findViewById(R.id.tvOngoingFilter), findViewById(R.id.tvUpcomingFilter), findViewById(R.id.tvCompletedFilter)};
        for (TextView chip : chips) {
            chip.setBackgroundTintList(ColorStateList.valueOf(softGrey));
            chip.setTextColor(hardGrey);
        }
    }

    private void fetchTreatmentsFromDb() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Fetch ALL treatments for this patient once
        db.collection("hospitals").document(hospitalId)
                .collection("treatments")
                .whereEqualTo("patient_id", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    fullList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Treatment t = doc.toObject(Treatment.class);
                            t.setId(doc.getId());
                            fullList.add(t);
                        }
                    }
                    applyFilterAndRefreshList();
                });
    }

    private void applyFilterAndRefreshList() {
        List<Treatment> filteredList = new ArrayList<>();

        if (selectedFilter.equals("all")) {
            filteredList.addAll(fullList);
        }
        else if(selectedFilter.equals("completed")){
            for (Treatment t : fullList) {
                if (t.status != null && (t.status.equalsIgnoreCase(TreatmentStatus.SUCCESS.name()) || t.status.equalsIgnoreCase(TreatmentStatus.FAILED.name()) )) {
                    filteredList.add(t);
                }
            }
        }
        else {
            for (Treatment t : fullList) {
                // Ensure treatment model has status field. Matches TreatmentStatus enum names
                if (t.status != null && t.status.equalsIgnoreCase(selectedFilter)) {
                    filteredList.add(t);
                }
            }
        }
        adapter.setList(filteredList);
    }

    @Override
    public void onDeleteClick(Treatment treatment) {
        // Patients usually can't delete treatments, just cancel appointments if needed
        Toast.makeText(this, "Cancellation not available here", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onDetailsClick(Treatment treatment){
        Intent intent =new Intent(this, TreatmentDetails.class);
        intent.putExtra("treatment_id", treatment.getId());
        intent.putExtra("hospital_id", hospitalId);
        startActivity(intent);
    }
}