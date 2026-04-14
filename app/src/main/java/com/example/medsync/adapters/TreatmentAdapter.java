package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Treatment;
import com.example.medsync.model.enums.TreatmentStatus;
import com.example.medsync.model.enums.TreatmentType;
import java.util.List;

public class TreatmentAdapter extends RecyclerView.Adapter<TreatmentAdapter.TreatmentViewHolder> {

    private List<Treatment> treatmentList;

    public TreatmentAdapter(List<Treatment> treatmentList) {
        this.treatmentList = treatmentList;
    }

    @NonNull
    @Override
    public TreatmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_treatment_card, parent, false);
        return new TreatmentViewHolder(view);
    }

    // Inside TreatmentAdapter.java - onBindViewHolder

    @Override
    public void onBindViewHolder(@NonNull TreatmentViewHolder holder, int position) {
        Treatment t = treatmentList.get(position);TreatmentType type = TreatmentType.fromString(t.type);
        TreatmentStatus status = TreatmentStatus.fromString(t.status);

        holder.tvType.setText(type.getDisplayName());
        holder.tvStatus.setText(status.getDisplayName());
        holder.tvDate.setText(t.start != null ? t.start : "No Date");

        // Displaying doctor name (assuming examiner_id contains the name or you want to label it as Doctor)
        holder.tvDocName.setText(t.examiner_name != null ? t.examiner_name : "Unassigned Doctor");

        // Status Colors
        int color;
        switch (status) {
            case SUCCESS: color = 0xFF2E7D32; break;
            case ONGOING: color = 0xFFEF6C00; break;
            case FAILED:  color = 0xFFC62828; break;
            default:      color = 0xFF757575; break;
        }
        holder.tvStatus.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return treatmentList != null ? treatmentList.size() : 0;
    }

    public static class TreatmentViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvStatus, tvDate, tvDocName;

        public TreatmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvTreatmentType);
            tvStatus = itemView.findViewById(R.id.tvStatusBadge);
            tvDate = itemView.findViewById(R.id.tvTreatmentDate);
            tvDocName = itemView.findViewById(R.id.tvDoctorName);
        }
    }
}