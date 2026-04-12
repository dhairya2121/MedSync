package com.example.medsync.model;
import java.util.List;

public class Doctor {

    public String hospital_id;
    public String specialization;//specializationType
    public String reg_no;

    public List<TimeSlot> working_slots; // ["09:00","09:30"]

    public Doctor() {}

}
