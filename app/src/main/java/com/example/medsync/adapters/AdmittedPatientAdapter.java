package com.example.medsync.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.activities.receptionist.AdmittedPatientDetails;
import com.example.medsync.model.Patient;
import java.util.ArrayList;
import java.util.List;

public class AdmittedPatientAdapter extends RecyclerView.Adapter<AdmittedPatientAdapter.ViewHolder> {

    private List<Patient> admittedList = new ArrayList<>();

    public void setList(List<Patient> list) {
        this.admittedList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admitted_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient item = admittedList.get(position);

        // 1. Display Patient Name
        String name = item.name;
        holder.tvPatientName.setText(name != null ? name : "Unknown Patient");
// ... inside onBindViewHolder ...

        // 2. Display Admission Date (Formatted as Admitted on dd month)
        holder.tvTreatmentType.setText("Admitted on " + item.admittedOn);

        // 3. Display Room Number (Room Badge)
        holder.tvRoomBadge.setText("Room " + item.room_no);


        // 4. Set Profile Initial
        if (name != null && !name.isEmpty()) {
            holder.tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            holder.tvProfileInitial.setText("P");
        }

        // 5. Display Subtitle (Gender | 00 yrs)
        String gender = item.gender != null ? item.gender : "N/A";
        long age = item.age;
        holder.tvPatientDetails.setText(String.format("%s | %d yrs", gender, age));
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AdmittedPatientDetails.class);
            intent.putExtra("patient_id", item.id);
            v.getContext().startActivity(intent);
        });

    }

    @Override
    public int getItemCount() { return admittedList.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProfileInitial, tvPatientName, tvTreatmentType, tvPatientDetails, tvRoomBadge;

        ViewHolder(View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvTreatmentType = itemView.findViewById(R.id.tvTreatmentType);
            tvPatientDetails = itemView.findViewById(R.id.tvPatientDetails);
            tvRoomBadge = itemView.findViewById(R.id.tvRoomBadge);
        }
    }
}