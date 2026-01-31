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
import android.widget.ImageView;
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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    
    EditText etEmail, etPass, etFirstName, etLastName, etAge;
    Button btnMainLogin, btnMainRegister, btnReg, btnLogin, btnAddPost, btnAllPost, btnMypost, btnDeleteProfile, btnEditProfile;
    private FirebaseAuth mAuth;
    private Dialog d;
    private ProgressDialog progressDialog;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference userRef;
    private Spinner spinnerGender;
    TextView tvProfileWelcome, tvWelcome;
    private ImageView ivProfilePreview;
    private String selectedImageBase64 = null;
    private ShapeableImageView ivMainProfile;
    private ValueEventListener currentUserListener;
    private DatabaseReference currentUserRef;
    
    private NotificationHelper notificationHelper;
    private boolean isFirstLoad = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth
        firebaseDatabase = FirebaseDatabase.getInstance(); // Initialize FirebaseDatabase
        progressDialog = new ProgressDialog(this);
        notificationHelper = new NotificationHelper(this);

        btnMainLogin = (Button) findViewById(R.id.btnLogin);
        btnMainRegister = (Button) findViewById(R.id.btnRegister);
        btnAddPost = (Button) findViewById(R.id.btnAddPost);
        btnAllPost = (Button) findViewById(R.id.btnAllPost);
        btnMypost = (Button) findViewById(R.id.btnMyPost);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        tvProfileWelcome = findViewById(R.id.tvProfileWelcome);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivMainProfile = findViewById(R.id.ivUserProfileMain);

        btnMainLogin.setOnClickListener(this);
        btnMainRegister.setOnClickListener(this);
        btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());

        ivMainProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
        });

        btnAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddPostActivity.class));
            }
        });
        btnAllPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AllPostActivity.class);
                intent.putExtra("showMyPosts", false); // Explicitly show all posts
                startActivity(intent);
            }
        });

        btnMypost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AllPostActivity.class);
                intent.putExtra("showMyPosts", true); // Show only my posts
                startActivity(intent);
            }
        });

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        });

        if (btnDeleteProfile != null) {
            btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        requestNotificationPermission(); // Request permission for Android 13+
        setupPostsListener();//setup posts listener
        checkUserConnectedStatus();//check if user is connected or not

    }

    private void requestNotificationPermission() {
        // function for request notification permission from android 13+ operation system

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupPostsListener() {
        DatabaseReference postsRef = firebaseDatabase.getReference("Posts");
        postsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isFirstLoad) {
                    Post post = snapshot.getValue(Post.class);
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    
                    if (post != null && currentUser != null && !post.uid.equals(currentUser.getUid()))
                    {
                        notificationHelper.showNewPostNotification(post);
                        Log.d("notificationHelper", "New post notification shown");
                    }
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFirstLoad = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDeleteConfirmationDialog()
    //function for create delete profile dialog
    {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteCurrentUserAccount();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteCurrentUserAccount()
    //function to delete user account->only after log-in
    {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        progressDialog.setMessage("Deleting account...");
        progressDialog.show();

        // Step 1: Delete user's posts
        DatabaseReference postsRef = firebaseDatabase.getReference("Posts");
        Query userPostsQuery = postsRef.orderByChild("uid").equalTo(uid);
        userPostsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    postSnapshot.getRef().removeValue();
                }

                // Step 2: Delete user's profile data from Realtime DB
                DatabaseReference userProfileRef = firebaseDatabase.getReference("Users").child(uid);
                userProfileRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Step 3: Delete user from Authentication
                        currentUser.delete().addOnCompleteListener(authTask -> {
                            progressDialog.dismiss();
                            if (authTask.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                Log.d("DeleteUser", "User account deleted successfully.");
                                checkUserConnectedStatus(); // Update UI
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to delete account auth.", Toast.LENGTH_SHORT).show();
                                Log.e("DeleteUser", "Failed to delete user account from auth.", authTask.getException());
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Failed to delete user profile.", Toast.LENGTH_SHORT).show();
                        Log.e("DeleteUser", "Failed to delete user profile from DB.", task.getException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Log.e("DeleteUser", "Failed to delete user posts.", databaseError.toException());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            //  reload();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnMainLogin)//check button status if login or logout
        {
            if (btnMainLogin.getText().toString().equals("Login"))//if the text written in  the button is login

            {
                createLoginDialog();
            } else if (btnMainLogin.getText().toString().equals("Logout"))//if the text written in  the button is logout
            {
                showLogoutConfirmationDialog();
            }

        } else if (v == btnMainRegister) {
            createRegisterDialog();

        } else if (btnReg == v) //btnRegister is the button in the dialog pop-up
        {
            register();

        } else if (v == btnLogin)//btnLogin is the login button in the dialog pop-up
        {
            login();
        }


    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateUserOnlineStatus(false); // Set offline status in DB
                        removeUserDetailsListener(); // Stop listening BEFORE sign out
                        FirebaseAuth.getInstance().signOut();//sign out
                        Log.d("TAG", "sign out");
                        Toast.makeText(MainActivity.this, "Logout success.",
                                Toast.LENGTH_SHORT).show();
                        checkUserConnectedStatus();
                    }
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
            String uid = currentUser.getUid();
            DatabaseReference userStatusRef = firebaseDatabase.getReference("Users").child(uid).child("isOnline");
            userStatusRef.setValue(isOnline);
            
            if (isOnline) {
                // Handle unexpected disconnection (e.g. app crash, network loss)
                userStatusRef.onDisconnect().setValue(false);
            }
        }
    }

    public void adduserDetailsInDB() {
        //function to add user details to DB
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e("adduserDetailsInDB", "Current user is null, cannot add details to DB.");
                return;
            }
            String uid = currentUser.getUid();
            int age = Integer.parseInt(etAge.getText().toString());
            String gender = spinnerGender.getSelectedItem().toString();

            // Create user with isOnline = true
            User userObj1 = new User(uid, etEmail.getText().toString(), etFirstName.getText().toString(), etLastName.getText().toString(), age, uid, gender, true);

            // Use UID as the key for the user
            userRef = firebaseDatabase.getReference("Users").child(uid);
            userRef.setValue(userObj1).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("TAG", "User details saved successfully in DB");
                    
                    // Set up onDisconnect even for fresh registrations
                    userRef.child("isOnline").onDisconnect().setValue(false);

                    // If an image was selected, save it under Users/{uid}/profileImage as Base64 string
                    if (selectedImageBase64 != null) {
                        userRef.child("profileImage").setValue(selectedImageBase64)
                                .addOnCompleteListener(imgTask -> {
                                    if (imgTask.isSuccessful()) {
                                        Log.d("TAG", "Profile image saved in Realtime DB");
                                    } else {
                                        Log.e("TAG", "Failed to save profile image", imgTask.getException());
                                    }                                });
                    }

                } else {
                    Log.e("TAG", "Failed to save user details", task.getException());
                    Toast.makeText(this, "Failed to save user details.", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Age must be a valid number.", Toast.LENGTH_SHORT).show();
            Log.e("adduserDetailsInDB", "NumberFormatException for age", e);
        } catch (Exception e) {
            Toast.makeText(this, "An error occurred while saving user details.", Toast.LENGTH_SHORT).show();
            Log.e("adduserDetailsInDB", "Error saving user details", e);
        }
    }

    private void updateProfileImage(String base64Image) {
        //function to update user profile image

        FirebaseUser currentUser = mAuth.getCurrentUser();//check if user is logged in
        if (currentUser == null) {
            return; // Should not happen if called correctly
        }
        String uid = currentUser.getUid();
        DatabaseReference userRef = firebaseDatabase.getReference("Users").child(uid);
        userRef.child("profileImage").setValue(base64Image)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Profile image updated.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to update profile image.", Toast.LENGTH_SHORT).show();
                        Log.e("UpdateImage", "Failed to update profile image in DB", task.getException());
                    }
                });
    }


    public void checkUserConnectedStatus() {
        //check if user is connected or not
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) { // User is connected
            updateUIForLoggedInUser();//function for update UI for logged in user
            updateUserOnlineStatus(true); // Ensure status is 'true' when connected
            String uid = firebaseUser.getUid();
            currentUserRef = firebaseDatabase.getReference("Users").child(uid);
            currentUserListener = currentUserRef.addValueEventListener(userDetailsListener);
        } else { // User is not connected
            removeUserDetailsListener();
            updateUIForLoggedOutUser();//function for update UI for logged out user
        }
    }

    private final ValueEventListener userDetailsListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            if (snapshot.exists()) {
                String firstname = snapshot.child("firstname").getValue(String.class);
                tvProfileWelcome.setText("Hello, " + firstname);
                tvProfileWelcome.setVisibility(View.VISIBLE);

                if (snapshot.hasChild("profileImage")) {
                    String imageBase64 = snapshot.child("profileImage").getValue(String.class);
                    try {
                        byte[] imageBytes = Base64.decode(imageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (ivMainProfile != null) {
                            ivMainProfile.setImageBitmap(bitmap);
                            ivMainProfile.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        Log.e("LoadImage", "Error decoding Base64 string", e);
                        if (ivMainProfile != null) {
                            ivMainProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                            ivMainProfile.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    if (ivMainProfile != null) {
                        ivMainProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                        ivMainProfile.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e("UserDetails", "Failed to read user details", error.toException());
            Toast.makeText(MainActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
        }
    };

    public void updateUIForLoggedInUser()
    {
        btnAddPost.setEnabled(true);
        btnAllPost.setEnabled(true);
        btnAllPost.setText("All Posts");
        btnMypost.setVisibility(View.VISIBLE);
        btnAddPost.setVisibility(View.VISIBLE);
        btnMainRegister.setVisibility(View.GONE);
        if (btnDeleteProfile != null) btnDeleteProfile.setVisibility(View.VISIBLE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.VISIBLE);
        btnMainLogin.setText("Logout");
        if (tvWelcome != null) tvWelcome.setVisibility(View.GONE);
    }

    public void updateUIForLoggedOutUser()
    {
        btnMypost.setVisibility(View.GONE);
        btnAllPost.setText("All Posts(Guest)");
        if (btnDeleteProfile != null) btnDeleteProfile.setVisibility(View.GONE);
        if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);
        btnAddPost.setVisibility(View.GONE);
        btnMainLogin.setText("Login");
        tvProfileWelcome.setVisibility(View.GONE);
        btnMainRegister.setVisibility(View.VISIBLE);
        if (tvWelcome != null) tvWelcome.setVisibility(View.VISIBLE);

        if (ivMainProfile != null) {
            ivMainProfile.setImageBitmap(null);
            ivMainProfile.setVisibility(View.GONE);
        }
    }

    private void createLoginDialog() {
        d = new Dialog(this);
        d.setContentView(R.layout.login_layout);//convert loginlayout xml to java object,connect between java and xml
        d.setTitle("Login");
        d.setCancelable(true);
        etEmail = (EditText) d.findViewById(R.id.etEmail);
        etPass = (EditText) d.findViewById(R.id.etPass);
        btnLogin = (Button) d.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);
        d.show();


    }

    private void login() {

        //function to login user
        progressDialog.setMessage("Login Please Wait...");
        progressDialog.show();
        String email = etEmail.getText().toString();
        String password = etPass.getText().toString();
        String TAG = "tag";
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(MainActivity.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                            checkUserConnectedStatus();
                            startActivity(new Intent(MainActivity.this, AllPostActivity.class));
//                            btnMainLogin.setText("Logout");
//                            btnMainRegister.setEnabled(false);//disable register button

                            // updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            // updateUI(null);
                        }
                        d.dismiss();//close dialog
                        progressDialog.dismiss();//close progress dialog

                    }
                });

    }

    public void createRegisterDialog() {
        d = new Dialog(this);
        d.setContentView(R.layout.registerlayout);//convert registerlayout xml to java object,connect between java and xml
        d.setTitle("Register");
        d.setCancelable(true);
        etEmail = (EditText) d.findViewById(R.id.etEmail);
        etPass = (EditText) d.findViewById(R.id.etPass);
        etAge = (EditText) d.findViewById(R.id.etAge);
        etFirstName = (EditText) d.findViewById(R.id.etFirstname);
        etLastName = (EditText) d.findViewById(R.id.etLastname);
        spinnerGender = (Spinner) d.findViewById(R.id.spinnerGender);
        btnReg = (Button) d.findViewById(R.id.btnRegister);
        btnReg.setOnClickListener(this);
        d.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                if (ivProfilePreview != null) {
                    ivProfilePreview.setImageBitmap(bitmap);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null && selectedImageBase64 != null) {
                    ivMainProfile.setImageBitmap(bitmap);
                    updateProfileImage(selectedImageBase64);
                }

            } catch (IOException e) {
                Log.e("ImagePick", "Failed to get bitmap from uri", e);
            }
        }
    }

    private void register() {
        //function to create user/register user
        String TAG = "tag";
        progressDialog.setMessage("Registering Please Wait...");
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(etEmail.getText().toString(), etPass.getText().toString())//have to be string and password have to be at least 6 characters
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(MainActivity.this, "Authentication success.",
                                    Toast.LENGTH_SHORT).show();
                            adduserDetailsInDB();//create new user json/table in Firebase
                            checkUserConnectedStatus();
                            startActivity(new Intent(MainActivity.this, AllPostActivity.class));
//                            btnMainLogin.setText("Logout");
                            //btnMainRegister.setEnabled(false);//disable register button

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                        d.dismiss();//close dialog
                        progressDialog.dismiss();//close progress dialog
                    }

                });
    }
}
