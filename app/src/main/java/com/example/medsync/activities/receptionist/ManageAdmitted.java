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
        setupBaseActivityNavbar("R", "Admitted Patients");
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
                    if (error != null || value == null) return;

                    List<Patient> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Patient p = new Patient();
                        p.id = doc.getId();
                        p.name = doc.getString("name");

                        // 1. Mapping Gender and Age for the Subtitle
                        p.gender = doc.getString("gender");
                        if (doc.contains("age")) {
                            p.age = doc.getLong("age");
                        }

                        // 2. Mapping Room Number
                        if (doc.contains("room_no")) {
                            p.room_no = doc.getLong("room_no");
                        }

                        // 3. Mapping the Date (Admitted on ...)
                        // We fetch the string from Firestore to match the Appointment style
                        // 3. Mapping the Date (Admitted on dd month)
                        String dateStr = doc.getString("admittedOn");
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                // Step 1: Parse the existing string format
                                // (Assuming your Firestore string is "yyyy-MM-dd" or "dd-MM-yyyy" - adjust accordingly)
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                java.util.Date date = inputFormat.parse(dateStr);

                                // Step 2: Format to "dd MMMM" (e.g., 14 April)
                                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM", Locale.getDefault());
                                p.admittedOn = outputFormat.format(date);
                            } catch (Exception e) {
                                // If parsing fails, use the raw string
                                p.admittedOn = dateStr;
                            }
                        } else {
                            p.admittedOn = "N/A";
                        }
                        list.add(p);
                    }
                    adapter.setList(list);
                });
    }
}