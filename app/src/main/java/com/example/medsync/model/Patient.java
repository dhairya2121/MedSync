package com.example.medsync.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Patient {
    public String name; // Add this
    public String email;
    public String phone;
    public long age=0;  // Add this
    public String doctor_id;
    public String gender="Male";
    public String assistant_id;
    public String hospital_id;
    @Exclude
    public String id;
    public boolean isAdmitted = false;
    public Timestamp admittedOn;
    public Timestamp dischargeOn;
    public String room_id;
    public Long room_no;
    public Patient() {}

    // Constructor for easy testing
    public Patient(String name, String gender, Long age, boolean isAdmitted) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.isAdmitted = isAdmitted;
    }

    public void setId(String id) {
        this.id=id;
    }
}