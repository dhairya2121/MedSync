package com.example.medsync.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.List;

public class Treatment {
    // Field for Firestore Document ID (not stored in DB fields)
    @Exclude
    private String id;

    public String type;
    public String hospital_id;
    public String room_id;

    // Names added for UI display (as used in Adapter)
    public String patient_id;
    public String patient_name;
    public String examiner_id;
    public String examiner_name; // Matches doctorName in adapter logic

    public String assistant_id;
    public Report report;
    public List<String> supplements;

    // Use Timestamp for Firebase compatibility with your adapter's date formatting
    private Timestamp timestamp;
    public String start;
    public String end;
    public String status;

    public Treatment() {}

    // Getters and Setters for the ID (Required by ManageAppointments)
    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    // Logic used by AppointmentAdapter
    public String getPatientName() { return patient_name != null ? patient_name : "Unknown Patient"; }
    public String getDoctorName() { return examiner_name != null ? examiner_name : "Unknown Doctor"; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}