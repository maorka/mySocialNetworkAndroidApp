package com.example.myfirebasetestapp1;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference postsDatabase;
    private ProgressDialog progressDialog;
    private NotificationHelper notificationHelper;

    private Button btnMainLogin, btnMainRegister;
    private TextView btnEditProfile;
    private com.google.android.material.floatingactionbutton.FloatingActionButton btnAddPost;
    private TextView tvProfileWelcome, tvWelcome;
    private ImageView ivMainProfile;
    private ListView lvMainPosts;
    private BottomNavigationView bottomNavigationView;
    private AllpostAdapter postAdapter;
    private ArrayList<Post> postsList;
    private LinearLayout llHeader;

    // Quick Post
    private com.google.android.material.card.MaterialCardView llQuickPostContainer;
    private EditText etQuickPost;
    private ImageButton btnQuickSend;

    // Search
    private ArrayList<Post> fullPostsList;
    private ImageButton btnChangeLang;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Get saved language and apply it
        String lang = LocaleHelper.getLanguage(newBase);
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        postsDatabase = firebaseDatabase.getReference("Posts");
        progressDialog = new ProgressDialog(this);
        notificationHelper = new NotificationHelper(this);

        btnMainLogin = findViewById(R.id.btnLogin);
        btnMainRegister = findViewById(R.id.btnRegister);
        btnAddPost = findViewById(R.id.btnAddPost);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvProfileWelcome = findViewById(R.id.tvProfileWelcome);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivMainProfile = findViewById(R.id.ivUserProfileMain);
        lvMainPosts = findViewById(R.id.lvMainPosts);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        llHeader = findViewById(R.id.llHeader);

        // Quick Post Init
        llQuickPostContainer = findViewById(R.id.llQuickPostContainer);
        etQuickPost = findViewById(R.id.etQuickPost);
        btnQuickSend = findViewById(R.id.btnQuickSend);

        btnChangeLang = findViewById(R.id.btnChangeLang);

        btnMainLogin.setOnClickListener(this);
        btnMainRegister.setOnClickListener(this);
        btnChangeLang.setOnClickListener(v -> showLanguageMenu());

        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddPostActivity.class));
        });

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        });

        btnQuickSend.setOnClickListener(v -> sendQuickPost());
        
        setupBottomNavigation();
        loadPosts();
        updateUI();
        updateBottomNavTitles();
    }

    private void showLanguageMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnChangeLang);
        popupMenu.getMenu().add(0, 1, 0, "English");
        popupMenu.getMenu().add(0, 2, 1, "עברית");

        popupMenu.setOnMenuItemClickListener(item -> {
            String lang = "en";
            if (item.getItemId() == 2) {
                lang = "iw";
            }
            
            if (!lang.equals(LocaleHelper.getLanguage(this))) {
                LocaleHelper.setLocale(this, lang);
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
            return true;
        });
        popupMenu.show();
    }

    private void updateBottomNavTitles() {
        bottomNavigationView.getMenu().findItem(R.id.nav_all_posts).setTitle(R.string.home);
        bottomNavigationView.getMenu().findItem(R.id.nav_search).setTitle(R.string.search);
        bottomNavigationView.getMenu().findItem(R.id.nav_favorites).setTitle(R.string.favorites);
        bottomNavigationView.getMenu().findItem(R.id.nav_my_posts).setTitle(R.string.my_profile);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_all_posts) {
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(this, AllPostActivity.class));
                return true;
            } else if (id == R.id.nav_favorites) {
                Intent intent = new Intent(this, AllPostActivity.class);
                intent.putExtra("showFavorites", true);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_my_posts) {
                if (mAuth.getCurrentUser() != null) {
                    startActivity(new Intent(this, EditProfileActivity.class));
                } else {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void loadPosts() {
        postsList = new ArrayList<>();
        fullPostsList = new ArrayList<>();
        postAdapter = new AllpostAdapter(this, 0, 0, postsList);
        lvMainPosts.setAdapter(postAdapter);

        postsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postsList.clear();
                fullPostsList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.key = postSnapshot.getKey();
                        postsList.add(post);
                        fullPostsList.add(post);
                    }
                }
                Collections.reverse(postsList);
                Collections.reverse(fullPostsList);
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error loading posts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            btnMainLogin.setVisibility(View.GONE);
            btnMainRegister.setVisibility(View.GONE);
            btnAddPost.setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.VISIBLE);
            tvProfileWelcome.setVisibility(View.VISIBLE);
            ivMainProfile.setVisibility(View.VISIBLE);
            tvWelcome.setVisibility(View.GONE);
            llQuickPostContainer.setVisibility(View.VISIBLE);
            btnChangeLang.setVisibility(View.VISIBLE);

            bottomNavigationView.getMenu().findItem(R.id.nav_favorites).setVisible(true);
            bottomNavigationView.getMenu().findItem(R.id.nav_my_posts).setVisible(true);

            DatabaseReference userRef = firebaseDatabase.getReference("Users").child(user.getUid());
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String firstName = snapshot.child("firstname").getValue(String.class);
                        String profileImage = snapshot.child("profileImage").getValue(String.class);
                        String displayName = (firstName != null && !firstName.isEmpty()) ? firstName : getString(R.string.default_user);
                        tvProfileWelcome.setText(getString(R.string.hello_user, displayName));
                        if (profileImage != null && !profileImage.isEmpty()) {
                            try {
                                byte[] imageBytes = Base64.decode(profileImage, Base64.DEFAULT);
                                Glide.with(MainActivity.this)
                                        .load(imageBytes)
                                        .into(ivMainProfile);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error decoding base64 image", e);
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            btnMainLogin.setVisibility(View.VISIBLE);
            btnMainRegister.setVisibility(View.VISIBLE);
            btnAddPost.setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.GONE);
            tvProfileWelcome.setVisibility(View.GONE);
            ivMainProfile.setVisibility(View.GONE);
            tvWelcome.setVisibility(View.VISIBLE);
            llQuickPostContainer.setVisibility(View.GONE);
            btnChangeLang.setVisibility(View.VISIBLE);

            bottomNavigationView.getMenu().findItem(R.id.nav_favorites).setVisible(false);
            bottomNavigationView.getMenu().findItem(R.id.nav_my_posts).setVisible(false);
        }
    }

    private void sendQuickPost() {
        String body = etQuickPost.getText().toString().trim();
        if (body.isEmpty()) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressDialog.setMessage("Posting...");
        progressDialog.show();

        firebaseDatabase.getReference("Users").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String authorName = snapshot.child("firstname").getValue(String.class);
                        String key = postsDatabase.push().getKey();
                        Post post = new Post(user.getUid(), "Quick Post", body, 0, key, authorName);
                        
                        if (key != null) {
                            postsDatabase.child(key).setValue(post).addOnCompleteListener(task -> {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    etQuickPost.setText("");
                                    Toast.makeText(MainActivity.this, "Posted!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnLogin) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (id == R.id.btnRegister) {
            startActivity(new Intent(this, RegisterActivity.class));
        }
    }
}