package com.example.medsync.model;

import java.util.List;

public class Treatment {

    public String type; // TreatmentType.java
    public String hospital_id;

    public String room_id;
    public String patient_id;
    public String examiner_id;
    public String assistant_id;

    public Report report; //Report.java
    public List<String> supplements;//Supplements.java

    public String start;
    public String end;

    public String status; // TreatmentStatus.java

    public Treatment() {}
}