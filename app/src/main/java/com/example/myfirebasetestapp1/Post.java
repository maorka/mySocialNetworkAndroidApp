package com.example.myfirebasetestapp1;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties// more flexible for chanages in DB
public class Post {
    public  String key;
    public String uid;//uid = user id
    public String title;
    public String authorFirstName;
    public int likes=0;
    public String body;
    public Map<String, Boolean> likers = new HashMap<>();
    public Map<String, Boolean> favoriters = new HashMap<>();
    public Object timestamp;
    public String postImage;

    @Exclude // This field will not be saved to Firebase
    public boolean isExpanded = false;//משתנה בוליאני עבור body

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String uid, String title, String body, int likes, String key, String authorFirstName) {
        this.key = key;
        this.uid = uid;//uid = user id
        this.title = title;
        this.likes = likes;
        this.body = body;
        this.authorFirstName = authorFirstName;//for display post author name
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public String getPostImage() {
        return postImage;
    }

    public void setPostImage(String postImage) {
        this.postImage = postImage;
    }
}
