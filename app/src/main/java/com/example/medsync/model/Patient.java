package com.example.medsync.model;
public class Patient {
    public String name; // Add this
    public String email;
    public String phone;
    public String age;  // Add this
    public String doctor_id;
    public String gender;
    public String assistant_id;
    public String hospital_id;
    public String id;
    public boolean isAdmitted = false;
    public String admittedOn;
    public String dischargeOn;
    public String room_id;

    public Patient() {}

    // Constructor for easy testing
    public Patient(String name, String gender, String age, boolean isAdmitted) {
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.isAdmitted = isAdmitted;
    }
}