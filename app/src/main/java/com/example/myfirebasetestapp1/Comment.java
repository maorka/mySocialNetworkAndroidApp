package com.example.myfirebasetestapp1;

public class Comment {
    public String uid;
    public String text;
    public long timestamp;
    public String key; // To store the comment's key

    public Comment() {
        // Default constructor required for calls to DataSnapshot.getValue(Comment.class)
    }

    public Comment(String uid, String text, long timestamp) {
        this.uid = uid;
        this.text = text;
        this.timestamp = timestamp;//for display post date and time hour
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
