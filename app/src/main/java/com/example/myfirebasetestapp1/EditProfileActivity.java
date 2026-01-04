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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etFirstName, etLastName, etAge;
    private Spinner spinnerGender;
    private Button btnSaveChanges, btnChangeProfileImage;
    private ShapeableImageView ivProfileImage;
    private DatabaseReference userRef;
    private String selectedImageBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFirstName = findViewById(R.id.etEditFirstName);
        etLastName = findViewById(R.id.etEditLastName);
        etAge = findViewById(R.id.etEditAge);
        spinnerGender = findViewById(R.id.spinnerEditGender);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnChangeProfileImage = findViewById(R.id.btnChangeProfileImage);
        ivProfileImage = findViewById(R.id.ivEditProfileImage);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());//get user data from firebase

        loadUserData();

        btnChangeProfileImage.setOnClickListener(v -> openFileChooser());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
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
                ivProfileImage.setImageBitmap(bitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                selectedImageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() //function to load user data
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);//read user user template from user class-היא לוקחת את המידע שכבר נשאב (ה-snapshot), ומשתמשת במחלקת User בתור תבנית כדי להפוך אותו מאוסף נתונים גולמי לאובייקט ג'אווה מסודר ונוח לשימוש.
                    if (user != null) {
                        etFirstName.setText(user.firstname);
                        etLastName.setText(user.lastname);
                        etAge.setText(String.valueOf(user.age));

                        if (user.gender != null) {
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerGender.getAdapter();
                            int spinnerPosition = adapter.getPosition(user.gender);
                            spinnerGender.setSelection(spinnerPosition);
                        }

                        if (user.profileImage != null) {
                            byte[] imageBytes = Base64.decode(user.profileImage, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            ivProfileImage.setImageBitmap(bitmap);
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
            updates.put("profileImage", selectedImageBase64);//update user profile image using key and value method
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> //function for update user data in firebase
        {

            if (task.isSuccessful()) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
