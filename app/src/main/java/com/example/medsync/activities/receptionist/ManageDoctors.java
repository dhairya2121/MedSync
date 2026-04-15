package com.example.medsync.activities.receptionist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Doctor;
import com.example.medsync.model.enums.SpecializationType;
import com.example.medsync.utils.BaseActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageDoctors extends BaseActivity {

    private FirebaseFirestore db;
    private String hospitalId;
    private RecyclerView rvDepartments;
    private DepartmentAdapter adapter;
    private final Map<String, List<Doctor>> doctorGroups = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_doctors);

        setupBaseActivityNavbar("R", "Manage Doctors");
        setupBaseActivityFooter("home", "R");

        db = FirebaseFirestore.getInstance();
        hospitalId = getSharedPreferences("medsync_prefs", MODE_PRIVATE).getString("hospital_id", "");

        rvDepartments = findViewById(R.id.rvDepartments);
        rvDepartments.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DepartmentAdapter();
        rvDepartments.setAdapter(adapter);

        fetchDoctors();
    }

    private void fetchDoctors() {
        db.collection("doctors")
                .whereEqualTo("hospital_id", hospitalId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    doctorGroups.clear();
                    for (SpecializationType type : SpecializationType.values()) {
                        doctorGroups.put(type.getDisplayName(), new ArrayList<>());
                    }

                    for (var doc : value.getDocuments()) {
                        Doctor d = doc.toObject(Doctor.class);
                        if (d != null) {
                            d.setId(doc.getId());
                            String spec = d.specialization != null ? d.specialization : SpecializationType.GENERAL_PHYSICIAN.getDisplayName();
                            if (!doctorGroups.containsKey(spec)) doctorGroups.put(spec, new ArrayList<>());
                            doctorGroups.get(spec).add(d);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- Adapter for Departments (Sections) ---
    class DepartmentAdapter extends RecyclerView.Adapter<DepartmentViewHolder> {
        private final List<String> specs = new ArrayList<>();
        private final Map<Integer, Boolean> expandedStates = new HashMap<>();

        @NonNull
        @Override
        public DepartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_department_section, parent, false);
            return new DepartmentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DepartmentViewHolder holder, int position) {
            String specName = SpecializationType.values()[position].getDisplayName();
            List<Doctor> docs = doctorGroups.get(specName);

            holder.tvTitle.setText(specName + " Department");
            boolean isExpanded = expandedStates.getOrDefault(position, false);
            holder.rvDoctors.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.ivArrow.setRotation(isExpanded ? 180 : 0);

            // Inner RecyclerView setup
            holder.rvDoctors.setLayoutManager(new LinearLayoutManager(ManageDoctors.this));
            holder.rvDoctors.setAdapter(new DoctorCardAdapter(docs != null ? docs : new ArrayList<>()));

            holder.itemView.setOnClickListener(v -> {
                expandedStates.put(position, !isExpanded);
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() { return SpecializationType.values().length; }
    }

    static class DepartmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        View ivArrow;
        RecyclerView rvDoctors;
        DepartmentViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvDeptTitle);
            ivArrow = v.findViewById(R.id.ivArrow);
            rvDoctors = v.findViewById(R.id.rvDoctorsInDept);
        }
    }

    // --- Inner Adapter for individual Doctor Cards ---
    // --- Update the Inner Adapter in ManageDoctors.java ---
    class DoctorCardAdapter extends RecyclerView.Adapter<DoctorViewHolder> {
        private final List<Doctor> doctors;

        DoctorCardAdapter(List<Doctor> doctors) {
            this.doctors = doctors;
        }

        @NonNull @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new DoctorViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_doctor_card, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder h, int p) {
            Doctor d = doctors.get(p);
            h.name.setText("Dr. " + d.name);
            h.spec.setText(d.specialization);
            h.exp.setText("Exp. " + d.exp + " years");
            h.fees.setText("Fees Rs. " + d.appointmentFee);
            h.initial.setText(d.name != null && !d.name.isEmpty() ? d.name.substring(0,1).toUpperCase() : "D");

            // --- ADD CLICK LISTENER HERE ---
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ManageDoctors.this, DoctorDetails.class);
                intent.putExtra("doctor_id", d.id);
                intent.putExtra("doctor_name", d.name);
                startActivity(intent);
            });
        }
        @Override
        public int getItemCount() { return doctors.size(); }
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