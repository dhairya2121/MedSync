package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.medsync.R;
import com.example.medsync.model.Treatment; // Ensure path is correct
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Treatment> treatments = new ArrayList<>();
    private final OnAppointmentListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy | hh:mm a", Locale.getDefault());

    public interface OnAppointmentListener {
        void onDeleteClick(Treatment treatment);
    }

    public AppointmentAdapter(OnAppointmentListener listener) {
        this.listener = listener;
    }

    public void setTreatments(List<Treatment> treatments) {
        this.treatments = treatments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
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

//        Glide.with(holder.itemView.getContext())
//                .load(treatment.getPatientImageUrl())
//                .placeholder(R.drawable.ic_launcher_background)
//                .into(holder.patientImage);



// Change it to:
        String pName = treatment.getPatientName();
        if (pName != null && !pName.isEmpty()) {
            holder.tvProfileInitial.setText(String.valueOf(pName.charAt(0)).toUpperCase());
        }
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(treatment));
    }

    @Override
    public int getItemCount() { return treatments.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName, doctorInfo, dateTime,tvProfileInitial;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvProfileInitial = itemView.findViewById(R.id.tvProfileInitial);
            patientName = itemView.findViewById(R.id.patientName);
            doctorInfo = itemView.findViewById(R.id.doctorInfo);
            dateTime = itemView.findViewById(R.id.appointmentDateTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}