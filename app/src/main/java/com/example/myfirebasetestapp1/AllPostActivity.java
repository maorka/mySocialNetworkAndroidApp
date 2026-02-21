package com.example.myfirebasetestapp1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Collections;

public class AllPostActivity extends AppCompatActivity {
    ListView lv;
    ArrayList<Post> posts;
    AllpostAdapter allpostAdapter;
    private DatabaseReference database;
    private Button btnBackToMenu;
    private TextView tvProfileWelcomeAll;
    private FirebaseAuth mAuth;
    private EditText etQuickPost;
    private ImageButton btnQuickSend;
    private LinearLayout llQuickPost;
    private String userFirstName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_post);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Posts");
        lv = (ListView) findViewById(R.id.lv);
        btnBackToMenu = (Button) findViewById(R.id.btnBackToMenu);
        tvProfileWelcomeAll = (TextView) findViewById(R.id.tvProfileWelcomeAll);
        etQuickPost = (EditText) findViewById(R.id.etQuickPost);
        btnQuickSend = (ImageButton) findViewById(R.id.btnQuickSend);
        llQuickPost = (LinearLayout) findViewById(R.id.llQuickPost);

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnQuickSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendQuickPost();
            }
        });

        checkUserStatus();
        this.retrievedata();
    }

    private void sendQuickPost() {
        // function to send quick post,logic
        String message = etQuickPost.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String key = database.push().getKey();
            
            String title, body;
            // logic for title and body based on message length
            if (message.length() <= 20)
            {
                title = message;
                body = "";
            } else {
                title = "Post";
                body = message;
            }

            Post quickPost = new Post(uid, title, body, 0, key, userFirstName);
            
            if (key != null) {
                database.child(key).setValue(quickPost).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        etQuickPost.setText("");
                        Log.d("QuickPost", "Quick post sent successfully");
                        Toast.makeText(AllPostActivity.this, "Posted!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("QuickPost", "Failed to send quick post", task.getException());
                        Toast.makeText(AllPostActivity.this, "Failed to post", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            llQuickPost.setVisibility(View.VISIBLE);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userFirstName = snapshot.child("firstname").getValue(String.class);
                        tvProfileWelcomeAll.setText("Hello, " + userFirstName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else {
            tvProfileWelcomeAll.setText("Hello, Guest");
            llQuickPost.setVisibility(View.GONE);
        }
    }

    private void retrievedata() {
        Intent intent = getIntent();
        boolean showMyPosts = intent.getBooleanExtra("showMyPosts", false);
        boolean showFavorites = intent.getBooleanExtra("showFavorites", false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserUid = (currentUser != null) ? currentUser.getUid() : null;

        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                posts = new ArrayList<Post>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post p = dataSnapshot.getValue(Post.class);
                    if (p != null) {
                        if (showMyPosts) {
                            if (currentUserUid != null && p.uid.equals(currentUserUid)) {
                                posts.add(p);
                            }
                        } else if (showFavorites) {
                            if (currentUserUid != null && p.favoriters != null && p.favoriters.containsKey(currentUserUid)) {
                                posts.add(p);
                            }
                        } else {
                            posts.add(p);
                        }
                    }
                }
                Collections.reverse(posts);
                allpostAdapter = new AllpostAdapter(AllPostActivity.this, 0, 0, posts);
                lv.setAdapter(allpostAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AllPostActivity", "Data retrieval cancelled", databaseError.toException());
            }
        });
    }
}
