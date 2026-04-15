package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Treatment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Treatment> treatments = new ArrayList<>();
    private final OnAppointmentListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy | hh:mm a", Locale.getDefault());

    // Define constants for View Types
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_CAN_ADMIT = 1;

    public interface OnAppointmentListener {
        void onDeleteClick(Treatment treatment);
        void onAdmitClick(Treatment treatment); // New listener method
    }

    public AppointmentAdapter(OnAppointmentListener listener) {
        this.listener = listener;
    }

    public void setTreatments(List<Treatment> treatments) {
        this.treatments = treatments;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Treatment t = treatments.get(position);
        // Logic: If status is "SUCCESS" (suggested for admission), show admit button layout
        if ("SUCCESS".equalsIgnoreCase(t.status)) {
            return TYPE_CAN_ADMIT;
        }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_CAN_ADMIT)
                ? R.layout.item_success_appointment_can_admit
                : R.layout.item_appointment;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Treatment treatment = treatments.get(position);

        holder.patientName.setText(treatment.getPatientName());
        holder.doctorInfo.setText("with " + treatment.getDoctorName());

        if (treatment.getTimestamp() != null) {
            holder.dateTime.setText(sdf.format(treatment.getTimestamp().toDate()));
        }

        String pName = treatment.getPatientName();
        if (pName != null && !pName.isEmpty()) {
            holder.tvProfileInitial.setText(String.valueOf(pName.charAt(0)).toUpperCase());
        }

        // Handle buttons based on layout type
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(treatment));
        }
        if (holder.btnAdmit != null) {
            holder.btnAdmit.setOnClickListener(v -> listener.onAdmitClick(treatment));
        }
    }

    @Override
    public int getItemCount() { return treatments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName, doctorInfo, dateTime, tvProfileInitial;
        ImageButton btnDelete, btnAdmit;

        ViewHolder(View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            patientName = itemView.findViewById(R.id.patientName);
            doctorInfo = itemView.findViewById(R.id.doctorInfo);
            dateTime = itemView.findViewById(R.id.appointmentDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnAdmit = itemView.findViewById(R.id.btnAdmit); // Only exists in success layout
        }
    }
}
