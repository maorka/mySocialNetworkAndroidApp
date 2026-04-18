package com.example.myfirebasetestapp1;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    
    EditText etEmail, etPass, etFirstName, etLastName, etAge;
    Button btnMainLogin, btnMainRegister, btnReg, btnLogin;
    TextView btnEditProfile;
    FloatingActionButton btnAddPost;
    private FirebaseAuth mAuth;
    private Dialog d;
    private ProgressDialog progressDialog;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference userRef;
    private Spinner spinnerGender;
    TextView tvProfileWelcome, tvWelcome;
    private ShapeableImageView ivMainProfile;
    private ValueEventListener currentUserListener;
    private DatabaseReference currentUserRef;
    
    private NotificationHelper notificationHelper;
    private boolean isFirstLoad = true;

    // Posts related
    private ListView lvMainPosts;
    private ArrayList<Post> postsList;
    private AllpostAdapter postsAdapter;
    private DatabaseReference postsDatabase;
    private BottomNavigationView bottomNavigationView;
    private View llHeader;

    // Quick Post
    private View llQuickPostContainer;
    private EditText etQuickPost;
    private ImageButton btnQuickSend;
    private String userFirstName = "User";

    // Search related
    private View cvSearchContainer;
    private EditText etSearchInMain;
    private ImageButton btnClearSearchMain;
    private ArrayList<Post> fullPostsList;


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

        // Search Init
        cvSearchContainer = findViewById(R.id.cvSearchContainer);
        etSearchInMain = findViewById(R.id.etSearchInMain);
        btnClearSearchMain = findViewById(R.id.btnClearSearchMain);

        btnMainLogin.setOnClickListener(this);
        btnMainRegister.setOnClickListener(this);

        btnAddPost.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddPostActivity.class));
        });

//        btnEditProfile.setOnClickListener(v -> {
//            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
//        });

        btnQuickSend.setOnClickListener(v -> sendQuickPost());
        
        btnClearSearchMain.setOnClickListener(v -> etSearchInMain.setText(""));

        etSearchInMain.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPosts(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            FirebaseUser user = mAuth.getCurrentUser();
            
            // Hide search by default unless selected
            cvSearchContainer.setVisibility(View.GONE);
            
            if (id == R.id.nav_all_posts) {
                if (user != null) llQuickPostContainer.setVisibility(View.VISIBLE);
                retrievePosts(false, false);
                return true;
            } else if (id == R.id.nav_search) {
                llQuickPostContainer.setVisibility(View.GONE);
                cvSearchContainer.setVisibility(View.VISIBLE);
                retrievePosts(false, false); // Load all to filter
                return true;
            } else if (id == R.id.nav_favorites) {
                if (user != null) {
                    llQuickPostContainer.setVisibility(View.GONE);
                    retrievePosts(false, true);
                    return true;
                }
                return false;
            } else if (id == R.id.nav_my_posts) {
                if (user != null) {
                    startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
                    return true;
                }
                return false;
            }
            return false;
        });

        requestNotificationPermission(); 
        setupPostsListener();
        checkUserConnectedStatus();
    }

    private void filterPosts(String query) {
        if (fullPostsList == null) return;
        ArrayList<Post> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        if (lowerQuery.isEmpty()) {
            filtered.addAll(fullPostsList);
        } else {
            for (Post p : fullPostsList) {
                if ((p.title != null && p.title.toLowerCase().contains(lowerQuery)) ||
                    (p.body != null && p.body.toLowerCase().contains(lowerQuery))) {
                    filtered.add(p);
                }
            }
        }
        postsAdapter = new AllpostAdapter(MainActivity.this, 0, 0, filtered);
        lvMainPosts.setAdapter(postsAdapter);
    }

    private void sendQuickPost() {
        String message = etQuickPost.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write something", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference newPostRef = postsDatabase.push();
            String key = newPostRef.getKey();
            
            String title, body;
            if (message.length() <= 20) {
                title = message;
                body = "";
            } else {
                title = message.substring(0, 20) + "...";
                body = message;
            }

            Post quickPost = new Post(uid, title, body, 0, key, userFirstName);
            if (key != null) {
                newPostRef.setValue(quickPost).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        etQuickPost.setText("");
                        Toast.makeText(MainActivity.this, "Posted!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void retrievePosts(boolean showMyPosts, boolean showFavorites) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserUid = (currentUser != null) ? currentUser.getUid() : null;

        postsDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullPostsList = new ArrayList<>();
                postsList = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post p = dataSnapshot.getValue(Post.class);
                    if (p != null) {
                        fullPostsList.add(p);
                        if (showMyPosts && currentUserUid != null) {
                            if (p.uid.equals(currentUserUid)) postsList.add(p);
                        } else if (showFavorites && currentUserUid != null) {
                            if (p.favoriters != null && p.favoriters.containsKey(currentUserUid)) postsList.add(p);
                        } else if (!showMyPosts && !showFavorites) {
                            postsList.add(p);
                        }
                    }
                }
                Collections.reverse(fullPostsList);
                Collections.reverse(postsList);
                
                if (cvSearchContainer.getVisibility() == View.VISIBLE) {
                    filterPosts(etSearchInMain.getText().toString());
                } else {
                    postsAdapter = new AllpostAdapter(MainActivity.this, 0, 0, postsList);
                    lvMainPosts.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void setupPostsListener() {
        postsDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isFirstLoad) {
                    Post post = snapshot.getValue(Post.class);
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (post != null && currentUser != null && !post.uid.equals(currentUser.getUid())) {
                        notificationHelper.showNewPostNotification(post);
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        postsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFirstLoad = false;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onClick(View v) {
        if (v == btnMainLogin) {
            if (btnMainLogin.getText().toString().equals("Login")) createLoginDialog();
            else showLogoutConfirmationDialog();
        } else if (v == btnMainRegister) {
            createRegisterDialog();
        } else if (btnReg == v) {
            register();
        } else if (v == btnLogin) {
            login();
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    updateUserOnlineStatus(false);
                    removeUserDetailsListener();
                    mAuth.signOut();
                    checkUserConnectedStatus();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void removeUserDetailsListener() {
        if (currentUserRef != null && currentUserListener != null) {
            currentUserRef.removeEventListener(currentUserListener);
            currentUserRef = null;
            currentUserListener = null;
        }
    }

    private void updateUserOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userStatusRef = firebaseDatabase.getReference("Users").child(currentUser.getUid()).child("isOnline");
            userStatusRef.setValue(isOnline);
            if (isOnline) userStatusRef.onDisconnect().setValue(false);
        }
    }

    public void checkUserConnectedStatus() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            updateUIForLoggedInUser();
            updateUserOnlineStatus(true);
            currentUserRef = firebaseDatabase.getReference("Users").child(firebaseUser.getUid());
            currentUserListener = currentUserRef.addValueEventListener(userDetailsListener);
        } else {
            removeUserDetailsListener();
            updateUIForLoggedOutUser();
        }
        retrievePosts(false, false);
    }

    private final ValueEventListener userDetailsListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()) {
                userFirstName = snapshot.child("firstname").getValue(String.class);
                tvProfileWelcome.setText("Hello, " + userFirstName);
                if (snapshot.hasChild("profileImage")) {
                    String imageBase64 = snapshot.child("profileImage").getValue(String.class);
                    try {
                        byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        ivMainProfile.setImageBitmap(bitmap);
                        ivMainProfile.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        ivMainProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                }
            }
        }
        @Override public void onCancelled(@NonNull DatabaseError error) {}
    };

    public void updateUIForLoggedInUser() {
        btnAddPost.setVisibility(View.VISIBLE);
        btnMainRegister.setVisibility(View.GONE);
        btnEditProfile.setVisibility(View.VISIBLE);
        btnMainLogin.setText("Logout");
        tvWelcome.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        findViewById(R.id.bottomAppBar).setVisibility(View.VISIBLE);
        llHeader.setVisibility(View.VISIBLE);
        tvProfileWelcome.setVisibility(View.VISIBLE);
        lvMainPosts.setVisibility(View.VISIBLE);
        llQuickPostContainer.setVisibility(View.VISIBLE);
        cvSearchContainer.setVisibility(View.GONE);
        
        bottomNavigationView.getMenu().findItem(R.id.nav_favorites).setVisible(true);
        bottomNavigationView.getMenu().findItem(R.id.nav_my_posts).setVisible(true);
    }

    public void updateUIForLoggedOutUser() {
        btnEditProfile.setVisibility(View.GONE);
        btnAddPost.setVisibility(View.GONE);
        btnMainLogin.setText("Login");
        tvProfileWelcome.setVisibility(View.GONE);
        btnMainRegister.setVisibility(View.VISIBLE);
        tvWelcome.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        findViewById(R.id.bottomAppBar).setVisibility(View.VISIBLE);
        llHeader.setVisibility(View.VISIBLE);
        ivMainProfile.setVisibility(View.GONE);
        lvMainPosts.setVisibility(View.VISIBLE);
        llQuickPostContainer.setVisibility(View.GONE);
        cvSearchContainer.setVisibility(View.GONE);

        bottomNavigationView.getMenu().findItem(R.id.nav_favorites).setVisible(false);
        bottomNavigationView.getMenu().findItem(R.id.nav_my_posts).setVisible(false);
    }

    private void createLoginDialog() {
        d = new Dialog(this);
        d.setContentView(R.layout.login_layout);
        etEmail = d.findViewById(R.id.etEmail);
        etPass = d.findViewById(R.id.etPass);
        btnLogin = d.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);
        d.show();
    }

    private void login() {
        progressDialog.setMessage("Login Please Wait...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(etEmail.getText().toString(), etPass.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserConnectedStatus();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                    d.dismiss();
                    progressDialog.dismiss();
                });
    }

    public void createRegisterDialog() {
        d = new Dialog(this);
        d.setContentView(R.layout.registerlayout);
        etEmail = d.findViewById(R.id.etEmail);
        etPass = d.findViewById(R.id.etPass);
        etAge = d.findViewById(R.id.etAge);
        etFirstName = d.findViewById(R.id.etFirstname);
        etLastName = d.findViewById(R.id.etLastname);
        spinnerGender = d.findViewById(R.id.spinnerGender);
        btnReg = d.findViewById(R.id.btnRegister);
        btnReg.setOnClickListener(this);
        d.show();
    }

    private void register() {
        progressDialog.setMessage("Registering Please Wait...");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(etEmail.getText().toString(), etPass.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        adduserDetailsInDB();
                        checkUserConnectedStatus();
                    } else {
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                    d.dismiss();
                    progressDialog.dismiss();
                });
    }

    private void adduserDetailsInDB() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        User userObj = new User(uid, etEmail.getText().toString(), etFirstName.getText().toString(), etLastName.getText().toString(), Integer.parseInt(etAge.getText().toString()), uid, spinnerGender.getSelectedItem().toString(), true);
        firebaseDatabase.getReference("Users").child(uid).setValue(userObj);
    }
}
