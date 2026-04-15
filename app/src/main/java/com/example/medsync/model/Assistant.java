package com.example.medsync.model;import com.google.firebase.firestore.Exclude;

public class Assistant {
    @Exclude
    public String id;
    public String name;
    public String gender;
    public long age;
    public long exp;
    public String hospital_id;
    public String email;
    public String phone;

    public Assistant() {}

    @Exclude
    public void setId(String id) { this.id = id; }
}