package com.example.myfirebasetestapp1;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties// more flexible for chanages in DB
public class User {
    //user class for users in my app
    public String key;
    public String uid;//user uid
    public String email,firstname, lastname,gender, profileImage;//need to add gender option
    public int age;
    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public User(String uid, String email, String firstname,String lastname, int age,String key, String gender) {
        this.uid = uid;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.age = age;
        this.key = key;
        this.gender = gender;
    }



}
