package com.example.medsync.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.HashMap;
import java.util.Map;

public class Bill {
    @Exclude
    public String id;

    public String treatment_id;
    public String patient_id;
    public String hospital_id;

    public Map<String, Double> items = new HashMap<>();
    public double total_amount;
    public String status; // e.g., "PENDING","UNDER_VERIFICATION", "PAID"
    public Timestamp generated_at;

    public Bill() {}

    @Exclude
    public double calculateTotal() {
        double sum = 0;
        for (Double val : items.values()) {
            sum += val;
        }
        this.total_amount = sum;
        return sum;
    }
}