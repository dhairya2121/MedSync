// File: model/CareTaker.java
package com.example.medsync.model;
import com.google.firebase.firestore.Exclude;

public class CareTaker {
    @Exclude public String id;
    public String name;
    public String gender;
    public long age;
    public String email;
    public String phone;
    public String hospital_id;
    public String patient_id;
    public String patient_name;

    public CareTaker() {}
    @Exclude public void setId(String id) { this.id = id; }
}