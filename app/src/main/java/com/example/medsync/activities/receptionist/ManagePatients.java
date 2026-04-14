package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.adapters.PatientAdapter;
import com.example.medsync.model.Patient;
import com.example.medsync.utils.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ManagePatients extends BaseActivity {

    private RecyclerView rvPatientList;
    private PatientAdapter adapter;
    // Ensure displayList is also initialized (though you did this in setupRecyclerView)
    private List<Patient>displayList = new ArrayList<>();
    private TextView tvFilterEnabled, tvFilterDisabled;
    private EditText etSearch;
    private MaterialButton btnSearchGo;

    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private String hospitalId;
    private boolean isAdmittedFilterActive = false;
    private List<Patient> fullFetchedList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_patients);

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE)
                .getString("hospital_id", "");

        rvPatientList    = findViewById(R.id.rvPatientList);
        tvFilterEnabled  = findViewById(R.id.tvFilterEnabled);
        tvFilterDisabled = findViewById(R.id.tvFilterDisabled);
        etSearch         = findViewById(R.id.et_search);
        btnSearchGo      = findViewById(R.id.btn_search_go);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityFooter("home", "R");

        setupRecyclerView();
        setupFilterLogic();

        btnSearchGo.setOnClickListener(v -> performSearch());

        loadPatients(null); // initial load — all patients
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    // Inside ManagePatients.java
    private void setupRecyclerView() {
        displayList = new ArrayList<>();
        adapter = new PatientAdapter(displayList, patient -> {
            Intent intent = new Intent(ManagePatients.this, PatientDetails.class);
            intent.putExtra("patient_id", patient.id);
            intent.putExtra("patient_name", patient.name);
            startActivity(intent);
        });
        rvPatientList.setLayoutManager(new LinearLayoutManager(this));
        rvPatientList.setAdapter(adapter);
    }
    // ── Filter toggle — works on already-fetched list, NO extra Firestore query ─

    private void setupFilterLogic() {
        View.OnClickListener filterToggle = v -> {
            isAdmittedFilterActive = !isAdmittedFilterActive;

            tvFilterEnabled.setVisibility(isAdmittedFilterActive ? View.VISIBLE : View.GONE);
            tvFilterDisabled.setVisibility(isAdmittedFilterActive ? View.GONE : View.VISIBLE);

            // Just re-apply filter on whatever is already in fullFetchedList
            applyFilterAndRefresh();
        };

        tvFilterEnabled.setOnClickListener(filterToggle);
        tvFilterDisabled.setOnClickListener(filterToggle);
    }

    // ── Search — fires a new Firestore query ──────────────────────────────────

    private void performSearch() {
        String queryText = etSearch.getText().toString().trim();
        loadPatients(queryText.isEmpty() ? null : queryText);
    }

    // ── Firestore query (only hospital_id + optional name prefix) ─────────────
    //    NO isAdmitted filter here — we do that client-side to avoid index errors

    private void loadPatients(String searchName) {
        if (listenerRegistration != null) listenerRegistration.remove();

        if (hospitalId == null || hospitalId.isEmpty()) {
            Log.e("ManagePatients", "Hospital ID is missing!");
            return;
        }

        Query query = db.collection("patients")
                .whereEqualTo("hospital_id", hospitalId)
                .orderBy("name"); // single-field index — always works, no composite needed

        if (searchName != null && !searchName.isEmpty()) {
            query = query
                    .startAt(searchName)
                    .endAt(searchName + "\uf8ff");
        }

        listenerRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e("ManagePatients", "Firestore error: " + error.getMessage());
                return;
            }

            fullFetchedList.clear();
            if (snapshots != null) {
                fullFetchedList.addAll(snapshots.toObjects(Patient.class));
            }

            // After fetch, apply the current filter state client-side
            applyFilterAndRefresh();
        });
    }

    // ── Client-side filter + notify adapter ───────────────────────────────────

    private void applyFilterAndRefresh() {
        displayList.clear();

        if (isAdmittedFilterActive) {
            for (Patient p : fullFetchedList) {
                if (p.isAdmitted) { // make sure Patient has isAdmitted() getter
                    displayList.add(p);
                }
            }
        } else {
            displayList.addAll(fullFetchedList);
        }

        adapter.notifyDataSetChanged();
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) listenerRegistration.remove();
    }
}