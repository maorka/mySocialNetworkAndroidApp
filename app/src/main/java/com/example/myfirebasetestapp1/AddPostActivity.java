package com.example.myfirebasetestapp1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddPostActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int IMAGE_PICKER_REQUEST = 1;
    EditText etTitle, etBody;
    Button btnSave, btnSelectImage;
    ImageView ivPostImagePreview;
    FirebaseDatabase firebaseDatabase;
    private String selectedImageBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        firebaseDatabase = FirebaseDatabase.getInstance();
        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        btnSave = findViewById(R.id.btnSave);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivPostImagePreview = findViewById(R.id.ivPostImagePreview);

        btnSave.setOnClickListener(this);
        btnSelectImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSave) {
            savePost();
        } else if (v.getId() == R.id.btnSelectImage) {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        // Intent for Gallery/Files
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
                // From Gallery
                Uri imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (data.getExtras() != null && data.getExtras().get("data") != null) {
                // From Camera
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (bitmap != null) {
                ivPostImagePreview.setImageBitmap(bitmap);
                ivPostImagePreview.setVisibility(View.VISIBLE);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            }
        }
    }

    private void savePost() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DatabaseReference userRef = firebaseDatabase.getReference("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        String authorFirstName = user.firstname;
                        DatabaseReference postRef = firebaseDatabase.getReference("Posts").push();
                        String key = postRef.getKey();
                        Post newPost = new Post(uid, etTitle.getText().toString(), etBody.getText().toString(), 0, key, authorFirstName);

                        if (selectedImageBase64 != null) {
                            newPost.setPostImage(selectedImageBase64);
                        }

                        postRef.setValue(newPost).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(AddPostActivity.this, "Post created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddPostActivity.this, "Failed to create post", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddPostActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
