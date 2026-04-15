package com.example.medsync.model;

import com.google.firebase.firestore.Exclude;
import java.util.List;

public class Doctor {
    @Exclude
    public String id;
    public String name;
    public String hospital_id;
    public String specialization; // Matches SpecializationType enum string
    public String reg_no;
    public long exp;
    public long appointmentFee;
    public List<TimeSlot> working_slots;

    public Doctor() {}

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }
}