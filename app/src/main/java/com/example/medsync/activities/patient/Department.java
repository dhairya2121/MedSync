package com.example.medsync.activities.patient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.enums.Departments;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Department extends BaseActivity {

    private String departmentName;
    private RecyclerView rvDoctors;
    private DoctorAdapter adapter;
    private List<Doctor> doctorList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_doctors); // Reusing the layout since it has a RecyclerView

        // 1. Get Department Name from Intent
        departmentName = getIntent().getStringExtra("department_name");

        // 2. Setup UI
        applyEdgeToEdgePadding(findViewById(R.id.main));
        TextView tvTitle=findViewById(R.id.tvTitle);
        setupBaseActivityNavbar("P", "Patient");

        for (Departments d : Departments.values()) {
            if (d.symbol.equalsIgnoreCase(departmentName)) {
                tvTitle.setText(d.getDisplayName() + " Department");
                break;
            }
        }
        setupBaseActivityFooter("home", "P");

        db = FirebaseFirestore.getInstance();
        rvDoctors = findViewById(R.id.rvDepartments); // Reusing the ID from activity_manage_doctors
        rvDoctors.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DoctorAdapter(doctorList);
        rvDoctors.setAdapter(adapter);

        // Inside Department.java onCreate

        fetchDoctorsByDepartment();
    }

    private String formatDeptName(String name) {
        if (name == null || name.isEmpty()) return "Medical";
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void fetchDoctorsByDepartment() {
        if (departmentName == null) return;

        // Get the list of Enum names (e.g., ["NEUROLOGIST", "PSYCHIATRIST"])
        List<String> validSpecializations = Departments.getSpecializationsForDept(departmentName);

        if (validSpecializations.isEmpty()) {
            Toast.makeText(this, "No doctors available in this section", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query Firestore using 'whereIn' to match any of the specialization enum names
        db.collection("doctors")
                .whereIn("specialization", validSpecializations)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Department", "Error: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        doctorList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Doctor d = doc.toObject(Doctor.class);
                            d.setId(doc.getId());

                            // Local fix: if doctor stores raw enum name, map it to display name for UI
                            // Assuming Doctor model has a method to get display name or handle it here
                            try {
                                String rawSpec = doc.getString("specialization");
                                d.specialization = SpecializationType.valueOf(rawSpec).getDisplayName();
                            } catch (Exception ignored) {}

                            doctorList.add(d);
                        }
                        adapter.notifyDataSetChanged();

                        if (doctorList.isEmpty()) {
                            Toast.makeText(this, "No doctors found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // --- Adapter using the same ViewHolder pattern as ManageDoctors ---
    private class DoctorAdapter extends RecyclerView.Adapter<DoctorViewHolder> {
        private final List<Doctor> doctors;

        DoctorAdapter(List<Doctor> doctors) {
            this.doctors = doctors;
        }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_card, parent, false);
            return new DoctorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
            Doctor d = doctors.get(position);
            holder.name.setText("Dr. " + d.name);
            holder.spec.setText(d.specialization);
            holder.exp.setText("Exp. " + d.exp + " years");
            holder.fees.setText("Fees Rs. " + d.appointmentFee);
            holder.initial.setText(d.name != null && !d.name.isEmpty() ? d.name.substring(0,1).toUpperCase() : "D");

            holder.itemView.setOnClickListener(v -> {
                // Redirect to a Doctor Details or Booking activity
                 Intent intent = new Intent(Department.this, DoctorDetails.class);
                 intent.putExtra("doctor_id", d.id);
                 startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return doctors.size();
        }
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        TextView name, spec, exp, fees, initial;
        DoctorViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tvDoctorName);
            spec = v.findViewById(R.id.tvSpecialization);
            exp = v.findViewById(R.id.tvExp);
            fees = v.findViewById(R.id.tvFees);
            initial = v.findViewById(R.id.tvProfileInitial);
        }
    }
}