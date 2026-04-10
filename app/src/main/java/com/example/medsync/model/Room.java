package com.example.medsync.model;

import java.util.Random;

public class Room {

    public String type;
    public boolean isOccupied;

    public String patient_id;
    public String hospital_id;

    public String passkey;

    public Room(){}
    public Room(String type, boolean isOccupied, String patient_id, String hospital_id) {
        this.type = type;
        this.isOccupied = isOccupied;
        this.patient_id = patient_id;
        int passkey = new Random().nextInt(9000) + 1000;
        this.passkey = String.valueOf(passkey);
    }
}