package com.example.medsync.model;
import java.util.List;

public class Doctor {

    public String hospital_id;
    public String specialization;
    public String reg_no;

    public List<TimeSlot> working_slots; // ["morning","afternoon"]

    public Doctor() {}
    public static class TimeSlot {
        public String start; // "09:00"
        public String end;   // "09:30"

        public TimeSlot() {}
        public TimeSlot(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }
}
