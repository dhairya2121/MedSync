package com.example.medsync.model;

import java.util.List;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

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
    public List<Timestamp> booked_slots;

    public Doctor() {}

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }
}