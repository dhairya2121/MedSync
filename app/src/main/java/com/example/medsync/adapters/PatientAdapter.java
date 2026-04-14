package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Patient;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private List<Patient> patientList;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Patient patient);
    }

    public PatientAdapter(List<Patient> patientList, OnPatientClickListener listener) {
        this.patientList = patientList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure you have a layout named item_patient_card.xml or similar
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient_card, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Patient patient = patientList.get(position);

        holder.tvName.setText(patient.name != null ? patient.name : "Unknown");
        holder.tvGender.setText(patient.gender != null ? patient.gender : "N/A");
        holder.tvAge.setText((patient.age != null ? patient.age : "00") + " yrs");

        if (patient.isAdmitted) {
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        if (patient.name != null && !patient.name.isEmpty()) {
            holder.tvProfileInitial.setText(String.valueOf(patient.name.charAt(0)).toUpperCase());
        }

        holder.itemView.setOnClickListener(v -> listener.onPatientClick(patient));
    }

    @Override
    public int getItemCount() {
        return patientList != null ? patientList.size() : 0;
    }

    public static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView tvProfileInitial, tvName, tvGender, tvAge, tvStatus;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}