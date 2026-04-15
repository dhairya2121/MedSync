package com.example.medsync.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medsync.R;
import com.example.medsync.model.Assistant;
import java.util.List;

public class AssistantAdapter extends RecyclerView.Adapter<AssistantAdapter.ViewHolder> {
    private List<Assistant> assistants;
    private OnAssistantClickListener listener;

    public interface OnAssistantClickListener {
        void onAssistantClick(Assistant assistant);
    }

    public AssistantAdapter(List<Assistant> assistants, OnAssistantClickListener listener) {
        this.assistants = assistants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assistant a = assistants.get(position);
        holder.name.setText(a.name);
        holder.spec.setText("Medical Assistant");
        holder.exp.setText("Exp. " + a.exp + " yrs");
        holder.fees.setVisibility(View.GONE); // Assistants don't have separate fees in this UI
        holder.initial.setText(a.name != null && !a.name.isEmpty() ? a.name.substring(0,1).toUpperCase() : "A");
        holder.itemView.setOnClickListener(v -> listener.onAssistantClick(a));
    }

    @Override
    public int getItemCount() { return assistants.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, spec, exp, fees, initial;
        ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tvDoctorName);
            spec = v.findViewById(R.id.tvSpecialization);
            exp = v.findViewById(R.id.tvExp);
            fees = v.findViewById(R.id.tvFees);
            initial = v.findViewById(R.id.tvProfileInitial);
        }
    }
}