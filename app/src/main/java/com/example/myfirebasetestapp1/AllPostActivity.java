package com.example.myfirebasetestapp1;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class AllPostActivity extends LanguageActivity {
    ListView lv;
    ArrayList<Post> posts;
    AllpostAdapter allpostAdapter;
    private DatabaseReference database;
    private ImageButton btnBackToMenu, btnChangeLang;
    private TextView tvProfileWelcomeAll;
    private FirebaseAuth mAuth;
    private EditText etSearchPosts;
    private ImageButton btnClearSearch;
    private LinearLayout llSearchContainer;
    private String userFirstName = "User";
    private ArrayList<Post> allPostsList; // To keep original data for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_post);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Posts");
        lv = (ListView) findViewById(R.id.lv);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);
        btnChangeLang = findViewById(R.id.btnChangeLang);
        tvProfileWelcomeAll = (TextView) findViewById(R.id.tvProfileWelcomeAll);
        etSearchPosts = (EditText) findViewById(R.id.etSearchPosts);
        btnClearSearch = (ImageButton) findViewById(R.id.btnClearSearch);
        llSearchContainer = (LinearLayout) findViewById(R.id.llSearchContainer);

        if (btnChangeLang != null) {
            btnChangeLang.setOnClickListener(v -> showLanguageMenu(v));
        }

        btnBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }//finish->return to main menu
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etSearchPosts.setText("");
                }
            });
        }

        if (etSearchPosts != null) {
            etSearchPosts.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterPosts(s.toString());
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }

        checkUserStatus();
        this.retrievedata();
    }

    private void filterPosts(String query) {
        if (allPostsList == null) return;
        ArrayList<Post> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        if (lowerCaseQuery.isEmpty()) {
            filteredList.addAll(allPostsList);
        } else {
            for (Post post : allPostsList) {
                if ((post.title != null && post.title.toLowerCase().contains(lowerCaseQuery)) ||
                    (post.body != null && post.body.toLowerCase().contains(lowerCaseQuery)) ||
                    (post.authorFirstName != null && post.authorFirstName.toLowerCase().contains(lowerCaseQuery))) {
                    filteredList.add(post);
                }
            }
        }
        allpostAdapter = new AllpostAdapter(AllPostActivity.this, 0, 0, filteredList);
        lv.setAdapter(allpostAdapter);
    }

    private void checkUserStatus() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            if (llSearchContainer != null) llSearchContainer.setVisibility(View.VISIBLE);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        userFirstName = snapshot.child("firstname").getValue(String.class);
                        if (tvProfileWelcomeAll != null) {
                            String displayName = (userFirstName != null && !userFirstName.isEmpty()) ? userFirstName : getString(R.string.default_user);
                            tvProfileWelcomeAll.setText(getString(R.string.hello_user, displayName));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        } else {
            if (tvProfileWelcomeAll != null) {
                tvProfileWelcomeAll.setText(getString(R.string.hello_user, "Guest"));
            }
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
                allPostsList = new ArrayList<Post>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post p = dataSnapshot.getValue(Post.class);
                    if (p != null) {
                        if (showMyPosts) {
                            if (currentUserUid != null && p.uid.equals(currentUserUid)) {
                                allPostsList.add(p);
                            }
                        } else if (showFavorites) {
                            if (currentUserUid != null && p.favoriters != null && p.favoriters.containsKey(currentUserUid)) {
                                allPostsList.add(p);
                            }
                        } else {
                            allPostsList.add(p);
                        }
                    }
                }
                Collections.reverse(allPostsList);
                
                // If there's an active search, filter the new data
                String currentQuery = etSearchPosts.getText().toString();
                if (!currentQuery.isEmpty()) {
                    filterPosts(currentQuery);
                } else {
                    allpostAdapter = new AllpostAdapter(AllPostActivity.this, 0, 0, allPostsList);
                    lv.setAdapter(allpostAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AllPostActivity", "Data retrieval cancelled", databaseError.toException());
            }
        });
    }
}
