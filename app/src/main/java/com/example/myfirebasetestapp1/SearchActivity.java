package com.example.myfirebasetestapp1;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private ImageButton btnBackFromSearch, btnClearSearch;
    private ListView lvSearchResults;
    private ArrayList<Post> allPosts;
    private ArrayList<Post> filteredPosts;
    private AllpostAdapter searchAdapter;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnBackFromSearch = findViewById(R.id.btnBackFromSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        database = FirebaseDatabase.getInstance().getReference("Posts");
        allPosts = new ArrayList<>();
        filteredPosts = new ArrayList<>();
        
        btnBackFromSearch.setOnClickListener(v -> finish());
        
        btnClearSearch.setOnClickListener(v -> etSearchQuery.setText(""));

        fetchAllPosts();

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPosts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchAllPosts() {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allPosts.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        allPosts.add(post);
                    }
                }
                Collections.reverse(allPosts);
                filterPosts(etSearchQuery.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SearchActivity", "Error fetching posts", error.toException());
            }
        });
    }

    private void filterPosts(String query) {
        filteredPosts.clear();
        String lowerCaseQuery = query.toLowerCase().trim();
        
        if (lowerCaseQuery.isEmpty()) {
            filteredPosts.addAll(allPosts);
        } else {
            for (Post post : allPosts) {
                if ((post.title != null && post.title.toLowerCase().contains(lowerCaseQuery)) ||
                    (post.body != null && post.body.toLowerCase().contains(lowerCaseQuery))) {
                    filteredPosts.add(post);
                }
            }
        }
        
        if (searchAdapter == null) {
            searchAdapter = new AllpostAdapter(this, 0, 0, filteredPosts);
            lvSearchResults.setAdapter(searchAdapter);
        } else {
            searchAdapter.notifyDataSetChanged();
        }
    }
}
