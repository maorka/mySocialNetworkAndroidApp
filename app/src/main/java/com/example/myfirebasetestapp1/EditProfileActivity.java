package com.example.myfirebasetestapp1;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    private static final int IMAGE_PICKER_REQUEST = 1;
    private EditText etFirstName, etLastName, etAge;
    private Spinner spinnerGender;
    private Button btnSaveChanges, btnChangeProfileImage, btnDeleteProfile, btnMyPosts;
    private ImageButton btnBack;
    private ShapeableImageView ivProfileImage;
    private DatabaseReference userRef;
    private String selectedImageBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        progressDialog = new ProgressDialog(this);

        etFirstName = findViewById(R.id.etEditFirstName);
        etLastName = findViewById(R.id.etEditLastName);
        etAge = findViewById(R.id.etEditAge);
        spinnerGender = findViewById(R.id.spinnerEditGender);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangeProfileImage = findViewById(R.id.btnChangeProfileImage);
        ivProfileImage = findViewById(R.id.ivEditProfileImage);
        btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        btnBack = findViewById(R.id.btnBackFromEdit);
        btnMyPosts = findViewById(R.id.btnMyPosts);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = firebaseDatabase.getReference("Users").child(currentUser.getUid());

        loadUserData();

        btnChangeProfileImage.setOnClickListener(v -> openImagePicker());
        btnSaveChanges.setOnClickListener(v -> saveChanges());

        if (btnMyPosts != null) {
            btnMyPosts.setOnClickListener(v -> {
                Intent intent = new Intent(EditProfileActivity.this, AllPostActivity.class);
                intent.putExtra("showMyPosts", true);
                startActivity(intent);
            });
        }
        
        if (btnDeleteProfile != null) {
            btnDeleteProfile.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());//finish->return to main menu
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile?")
                .setPositiveButton("Yes", (dialog, which) -> deleteCurrentUserAccount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteCurrentUserAccount() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        progressDialog.setMessage("Deleting account...");
        progressDialog.show();

        DatabaseReference postsRef = firebaseDatabase.getReference("Posts");
        Query userPostsQuery = postsRef.orderByChild("uid").equalTo(uid);
        userPostsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    postSnapshot.getRef().removeValue();
                }

                DatabaseReference userProfileRef = firebaseDatabase.getReference("Users").child(uid);
                userProfileRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.delete().addOnCompleteListener(authTask -> {
                            progressDialog.dismiss();
                            if (authTask.isSuccessful()) {
                                Toast.makeText(EditProfileActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Failed to delete account auth.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(EditProfileActivity.this, "Failed to delete user profile.", Toast.LENGTH_SHORT).show();
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

    private void openImagePicker() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = Intent.createChooser(galleryIntent, "Select Image Source");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
        startActivityForResult(chooser, IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICKER_REQUEST && data != null) {
            Bitmap bitmap = null;
            if (data.getData() != null) {
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (data.getExtras() != null && data.getExtras().get("data") != null) {
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (bitmap != null) {
                ivProfileImage.setImageBitmap(bitmap);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            }
        }
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        etFirstName.setText(user.firstname);
                        etLastName.setText(user.lastname);
                        etAge.setText(String.valueOf(user.age));
                        if (user.gender != null) {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerGender.getAdapter();
                            int spinnerPosition = adapter.getPosition(user.gender);
                            spinnerGender.setSelection(spinnerPosition);
                        }
                        if (snapshot.hasChild("profileImage")) {
                            String imgBase64 = snapshot.child("profileImage").getValue(String.class);
                            if (imgBase64 != null) {
                                byte[] imageBytes = Base64.decode(imgBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                ivProfileImage.setImageBitmap(bitmap);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProfileActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveChanges() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();

        if (firstName.isEmpty() || lastName.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageStr);
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstname", firstName);
        updates.put("lastname", lastName);
        updates.put("age", age);
        updates.put("gender", gender);

        if (selectedImageBase64 != null) {
            updates.put("profileImage", selectedImageBase64);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
