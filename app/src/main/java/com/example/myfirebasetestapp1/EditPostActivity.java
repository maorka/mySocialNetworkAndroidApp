package com.example.myfirebasetestapp1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditPostActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int IMAGE_PICKER_REQUEST = 1;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private EditText etTitle, etBody;
    private Button btnSave, btnChangeImage;
    private ImageView ivPostImage;
    private DatabaseReference postRef;
    private Post currentPost;
    private String selectedImageBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        etTitle = findViewById(R.id.etTitle);
        etBody = findViewById(R.id.etBody);
        btnSave = findViewById(R.id.btnSave);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        ivPostImage = findViewById(R.id.ivEditPostImage);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnSave.setOnClickListener(this);
        btnChangeImage.setOnClickListener(this);

        String postKey = getIntent().getStringExtra("POST_KEY");
        if (postKey == null) {
            Toast.makeText(this, "Error: Post key not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        postRef = FirebaseDatabase.getInstance().getReference("Posts").child(postKey);
        loadPostData();
    }

    private void loadPostData() {
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentPost = dataSnapshot.getValue(Post.class);
                if (currentPost != null) {
                    etTitle.setText(currentPost.title);
                    etBody.setText(currentPost.body);
                    if (currentPost.postImage != null && !currentPost.postImage.isEmpty()) {
                        try {
                            byte[] imageBytes = Base64.decode(currentPost.postImage, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            ivPostImage.setImageBitmap(bitmap);
                            ivPostImage.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Log.e("EditPost", "Error decoding image", e);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditPostActivity.this, "Failed to load post.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSave) {
            savePostChanges();
        } else if (v.getId() == R.id.btnChangeImage) {
            checkPermissionAndOpenPicker();
        }
    }

    private void checkPermissionAndOpenPicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openImagePicker();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
                // We can still open the picker, but camera option might not work or we just open gallery
                openImagePicker();
            }
        }
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
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICKER_REQUEST) {
            Bitmap bitmap = null;

            if (data != null && data.getData() != null) {
                // From Gallery
                Uri imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                // From Camera
                bitmap = (Bitmap) data.getExtras().get("data");
            }

            if (bitmap != null) {
                ivPostImage.setImageBitmap(bitmap);
                ivPostImage.setVisibility(View.VISIBLE);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            }
        }
    }

    private void savePostChanges() {
        if (currentPost != null) {
            currentPost.title = etTitle.getText().toString();
            currentPost.body = etBody.getText().toString();

            if (selectedImageBase64 != null) {
                currentPost.postImage = selectedImageBase64;
            }

            postRef.setValue(currentPost).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditPostActivity.this, "Post updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditPostActivity.this, "Failed to update post", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
