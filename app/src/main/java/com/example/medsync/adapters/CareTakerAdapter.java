package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.CareTaker;
import java.util.List;

public class CareTakerAdapter extends RecyclerView.Adapter<CareTakerAdapter.ViewHolder> {

    private final List<CareTaker> careTakerList;
    private final OnCareTakerClickListener listener;

    public interface OnCareTakerClickListener {
        void onCareTakerClick(CareTaker careTaker);
    }

    public CareTakerAdapter(List<CareTaker> careTakerList, OnCareTakerClickListener listener) {
        this.careTakerList = careTakerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing item_patient_card for a consistent look
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CareTaker ct = careTakerList.get(position);

        holder.tvName.setText(ct.name != null ? ct.name : "Unknown");
        holder.tvGender.setText(ct.gender != null ? ct.gender : "N/A");
        holder.tvAge.setText(ct.age + " yrs");

        // Show assigned patient name as the status or extra info
        if (ct.patient_name != null && !ct.patient_name.isEmpty()) {
            holder.tvStatus.setText("for " + ct.patient_name);
            holder.tvStatus.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Set profile initial (e.g., 'A' for Alice)
        if (ct.name != null && !ct.name.isEmpty()) {
            holder.tvInitial.setText(ct.name.substring(0, 1).toUpperCase());
        }

        holder.itemView.setOnClickListener(v -> listener.onCareTakerClick(ct));
    }

    @Override
    public int getItemCount() {
        return careTakerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvGender, tvAge, tvInitial, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs match item_patient_card.xml
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvAge = itemView.findViewById(R.id.tvAge);
            tvInitial = itemView.findViewById(R.id.tvProfileInitial);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}