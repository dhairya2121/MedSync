package com.example.medsync.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TreatmentAdapter extends RecyclerView.Adapter<TreatmentAdapter.TreatmentViewHolder> {

    private List<Treatment> treatmentList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault());
    private OnTreatmentListener listener;

    private String role;

    public interface OnTreatmentListener {
        void onDeleteClick(Treatment treatment);
        void onDetailsClick(Treatment treatment);
    }

    // Single constructor to handle the listener
    public TreatmentAdapter(List<Treatment> treatmentList, OnTreatmentListener listener,String role) {
        this.treatmentList = treatmentList;
        this.listener = listener;
        this.role=role;
    }

    public void setList(List<Treatment> list) {
        this.treatmentList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TreatmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_treatment_card, parent, false);
        return new TreatmentViewHolder(view);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull TreatmentViewHolder holder, int position) {
        Treatment t = treatmentList.get(position);

        // 1. Title handling
        String displayTitle = t.type;
        try {
            displayTitle = TreatmentType.valueOf(t.type).getDisplayName();
        } catch (Exception e) {
            displayTitle = t.type;
        }
        holder.tvType.setText(displayTitle);

        // 2. Date Formatting
        // Create the requested format: "dd-MMM-yyyy, hh:mm a"
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale.getDefault());

        if (t.getTimestamp() != null) {
            // BEST WAY: Use the Firestore Timestamp directly
            holder.tvDate.setText(displayFormat.format(t.getTimestamp().toDate()));
        } else if (t.start != null) {
            // FALLBACK: Try to parse the 'start' string if timestamp is missing
            try {
                // Assuming t.start might be in ISO format "yyyy-MM-dd'T'HH:mm:ss"
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = parser.parse(t.start);
                if (date != null) {
                    holder.tvDate.setText(displayFormat.format(date));
                } else {
                    holder.tvDate.setText(t.start);
                }
            } catch (Exception e) {
                // If it's not an ISO string (e.g., just "09:00 AM"), show it as is
                holder.tvDate.setText(t.start);
            }
        } else {
            holder.tvDate.setText("Date not set");
        }

        // 3. Info and Visibility
        holder.tvDocName.setText(t.getDoctorName());
        holder.tvStatus.setText(t.status != null ? t.status.toUpperCase() : "PENDING");
        holder.llPatientRow.setVisibility(View.VISIBLE);
        holder.tvPatientName.setText(t.getPatientName());

        // 4. Status Coloring
        if ("SUCCESS".equalsIgnoreCase(t.status)) {
            holder.tvStatus.setTextColor(R.color.hard_green);
        } else if ("ONGOING".equalsIgnoreCase(t.status)) {
            holder.tvStatus.setTextColor(R.color.hard_green);
        } else {
            holder.tvStatus.setTextColor(0xFF757575);
        }

        if(role=="R"){
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(t);
                }
            });
        }
        else{
            holder.btnDelete.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetailsClick(t);
            }
        });

    }

    @Override
    public int getItemCount() {
        return treatmentList != null ? treatmentList.size() : 0;
    }

    public static class TreatmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvStatus, tvDate, tvDocName, tvPatientName;
        LinearLayout llPatientRow;
        ImageButton btnDelete;

        public TreatmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvTreatmentType);
            tvStatus = itemView.findViewById(R.id.tvStatusBadge);
            tvDate = itemView.findViewById(R.id.tvTreatmentDate);
            tvDocName = itemView.findViewById(R.id.tvDoctorName);
            llPatientRow = itemView.findViewById(R.id.llPatientRow);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}