package com.example.myfirebasetestapp1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AllPostActivity extends AppCompatActivity {
    ListView lv;
    ArrayList<Post> posts;
    AllpostAdapter allpostAdapter;
    private DatabaseReference database;
    private Button btnBackToMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_post);

        database = FirebaseDatabase.getInstance().getReference("Posts");//get reference from realtime firebase posts collecion/table
        lv = (ListView) findViewById(R.id.lv);
        btnBackToMenu = (Button) findViewById(R.id.btnBackToMenu);

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close this activity and go back to MainActivity
            }
        });

        this.retrievedata();
    }

    private void retrievedata() {

        Query query;
        Intent intent = getIntent();
        boolean showMyPosts = intent.getBooleanExtra("showMyPosts", false);// intent from myposts button pressed

        if (showMyPosts) {
            //check if specific user posts are needed
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();//get current user
            if (currentUser != null) {//check if user is logged in
                String currentUserUid = currentUser.getUid();//get current user uid
                query = database.orderByChild("uid").equalTo(currentUserUid);//query for specific user posts,filtering in db=>if currentUserUid = uid -> show specific user posts
            } else {
                // Handle the case where the user is not logged in
                return;
            }
        } else {
            query = database;
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) //function if success to get data from firebase
            //get data from firebase
            {
                posts = new ArrayList<Post>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) //get/scanning all data from firebase
                {
                    Post p = dataSnapshot.getValue(Post.class);
                    posts.add(p);//add post to posts list
                }
                allpostAdapter = new AllpostAdapter(AllPostActivity.this,0,0, posts);//create adapter for list view,0,0 because using custom_post xml
                lv.setAdapter(allpostAdapter);//set adapter to list view,get all posts and display them om the list

            }
            @Override
            public void onCancelled(DatabaseError databaseError) //function if failed to get data from firebase
            {

            }
        });

    }
}