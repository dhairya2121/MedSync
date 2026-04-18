package com.example.medsync.activities.doctor;

import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.adapters.PatientAdapter;
import com.example.medsync.model.Patient;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ManageAdmitted extends BaseActivity {
    //start this
    private RecyclerView rvAdmitted;
    private PatientAdapter adapter;
    private List<Patient> patientList = new ArrayList<>();
    private FirebaseFirestore db;
    private String doctorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_admitted);

        db = FirebaseFirestore.getInstance();
        doctorId = FirebaseAuth.getInstance().getUid();

        setupBaseActivityNavbar("D", "Admitted Patients");
        setupBaseActivityFooter("home", "D");
        applyEdgeToEdgePadding(findViewById(R.id.main));

        rvAdmitted = findViewById(R.id.rvAdmittedPatients);
        adapter = new PatientAdapter(patientList, patient -> {
            // Redirect to Patient Details or Treatment logic for doctor
        });
        rvAdmitted.setLayoutManager(new LinearLayoutManager(this));
        rvAdmitted.setAdapter(adapter);

        fetchAdmittedPatients();
    }

    private void fetchAdmittedPatients() {
        // First get doctor's hospital ID
        db.collection("doctors").document(doctorId).get().addOnSuccessListener(doc -> {
            String hospitalId = doc.getString("hospital_id");
            if (hospitalId != null) {
                db.collection("patients")
                        .whereEqualTo("hospital_id", hospitalId)
                        .whereEqualTo("isAdmitted", true)
                        .whereEqualTo("examiner_id", doctorId)
                        .addSnapshotListener((value, error) -> {
                            if (value != null) {
                                if(value.size()==0){
                                    Toast.makeText(this,"No patients under you",Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                                patientList.clear();
                                patientList.addAll(value.toObjects(Patient.class));
                                // Manually set IDs
                                for (int i = 0; i < value.size(); i++) {
                                    patientList.get(i).id = value.getDocuments().get(i).getId();
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });
            }
        });
    }
}