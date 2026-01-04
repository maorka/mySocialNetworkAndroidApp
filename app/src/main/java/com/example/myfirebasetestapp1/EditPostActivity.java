package com.example.myfirebasetestapp1;

import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditPostActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE_REQUEST = 1;
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
                        // Load the post image
                        byte[] imageBytes = Base64.decode(currentPost.postImage, Base64.DEFAULT);//convert base64 to image
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);//convert image to bitmap
                        ivPostImage.setImageBitmap(bitmap);
                        ivPostImage.setVisibility(View.VISIBLE);
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
        if (v.getId() == R.id.btnSave)//if pressed on save button
        {
            savePostChanges();
        } else if (v.getId() == R.id.btnChangeImage) //if pressed on change image button
        {
            openFileChooser();
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                ivPostImage.setImageBitmap(bitmap);
                ivPostImage.setVisibility(View.VISIBLE);

                // Convert the bitmap to Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            } catch (IOException e) {
                Log.e("EditPost", "Failed to load image from gallery", e);
            }
        }
    }

    private void savePostChanges()
            //function to save post changes
    {
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
