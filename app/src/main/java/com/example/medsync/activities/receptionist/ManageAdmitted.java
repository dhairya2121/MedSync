package com.example.medsync.activities.receptionist;

import static com.example.medsync.utils.ViewUtils.isValidUid;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.AdmittedPatientAdapter;
import com.example.medsync.model.Patient;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageAdmitted extends BaseActivity {

    private RecyclerView recyclerView;
    private AdmittedPatientAdapter adapter;
    private FirebaseFirestore db;
    private String hospitalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_admitted);

        applyEdgeToEdgePadding(findViewById(R.id.main));
        setupBaseActivityNavbar("R", "Receptionist");
        setupBaseActivityFooter("home", "R");

        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", null);
        if (!isValidUid(hospitalId)) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvAdmittedPatients);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdmittedPatientAdapter();
        recyclerView.setAdapter(adapter);

        fetchAdmittedPatients();
    }

    private void fetchAdmittedPatients() {
        db.collection("patients")
            .whereEqualTo("hospital_id", hospitalId)
            .whereEqualTo("isAdmitted", true)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    android.util.Log.e("ManageAdmitted", "Error: " + error.getMessage());
                    return;
                }

                if (value != null) {
                    List<Patient> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Patient p = doc.toObject(Patient.class);
                        if (p != null) {
                            p.setId(doc.getId());
                            list.add(p);
                        }
                    }
                    adapter.setList(list);
                }
            });
    }
}