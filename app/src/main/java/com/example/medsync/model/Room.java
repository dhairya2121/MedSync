package com.example.medsync.model;


public class Room {
    public String room_id;   // ✅ Add this — Firestore document ID
    public String type;
    public boolean isOccupied = false;
    public String patient_id;
    public String hospital_id;
    public long room_no;
    public String passkey;

    public Room() {}

    public Room(String type, boolean isOccupied, String patient_id, String hospital_id) {
        this.type        = type;
        this.isOccupied  = false;
        this.patient_id  = patient_id;
        this.hospital_id = hospital_id;
        int passkey      = new java.util.Random().nextInt(9000) + 1000;
        this.passkey     = String.valueOf(passkey);
    }
}